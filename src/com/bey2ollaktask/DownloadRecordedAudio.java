

package com.bey2ollaktask;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;


public class DownloadRecordedAudio extends AsyncTask<Void, Long, Boolean> {


    private Context mContext;
    private final ProgressDialog mDialog;
    private DropboxAPI<?> mApi;
    private String mPath;

    private OutputStream  mFos;

    private boolean mCanceled;
    private Long mFileLen;
    private String mErrorMsg;
    String fileName;
    MediaPlayer mPlayer;


    public DownloadRecordedAudio(Context context, DropboxAPI<?> api, String recordName,String fileName) {
        mContext = context.getApplicationContext();
        this.fileName = fileName;
        mApi = api;
        mPath = recordName;

        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Downloading recorded file");
        mDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mCanceled = true;
                mErrorMsg = "Canceled";

                
                if (mFos != null) {
                    try {
                        mFos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });

        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            if (mCanceled) {
                return false;
            }

          
            if (mCanceled) {
                return false;
            }

           //directory for downloaded folder
    		File direct = new File(Environment.getExternalStorageDirectory()
    				+ "/Bey2ollakRecord");

    		if (!direct.exists()) {
    			//create folder if not exists 
    			direct.mkdirs();
    		}
    		//directory for downloaded recorded file
    		File file = new File(Environment.getExternalStorageDirectory()
    				+ "/Bey2ollakRecord" + "/" + fileName);
    		if (!file.exists())
    			//download recorded audio if not exists
    		{
    			 try {
    	                mFos = new BufferedOutputStream(new FileOutputStream(file));
    	            } catch (FileNotFoundException e) {
    	                mErrorMsg = "Couldn't create a local file to store the recorded file";
    	                return false;
    	            }

    	           
    	            mApi.getFile(mPath, null, mFos, null);
    	            
    	            if (mCanceled) {
    	                return false;
    	            }

    	            	
    		}
    		return true;

        } catch (DropboxUnlinkedException e) {
            // The AuthSession wasn't properly authenticated or user unlinked.
        } catch (DropboxPartialFileException e) {
            // We canceled the operation
            mErrorMsg = "Download canceled";
        } catch (DropboxServerException e) {
            // Server-side exception.  These are examples of what could happen,
            // but we don't do anything special with them here.
            if (e.error == DropboxServerException._304_NOT_MODIFIED) {
                // won't happen since we don't pass in revision with metadata
            } else if (e.error == DropboxServerException._401_UNAUTHORIZED) {
                // Unauthorized, so we should unlink them.  You may want to
                // automatically log the user out in this case.
            } else if (e.error == DropboxServerException._403_FORBIDDEN) {
                // Not allowed to access this
            } else if (e.error == DropboxServerException._404_NOT_FOUND) {
                // path not found 
            } else if (e.error == DropboxServerException._406_NOT_ACCEPTABLE) {
                // too many entries to return
            } else if (e.error == DropboxServerException._415_UNSUPPORTED_MEDIA) {
                // can't be thumbnailed
            } else if (e.error == DropboxServerException._507_INSUFFICIENT_STORAGE) {
                // user is over quota
            } else {
                // Something else
            }
            // This gets the Dropbox error, translated into the user's language
            mErrorMsg = e.body.userError;
            if (mErrorMsg == null) {
                mErrorMsg = e.body.error;
            }
        } catch (DropboxIOException e) {
            // Happens all the time, probably want to retry automatically.
            mErrorMsg = "Network error.  Try again.";
        } catch (DropboxParseException e) {
            // Probably due to Dropbox server restarting, should retry
            mErrorMsg = "Dropbox error.  Try again.";
        } catch (DropboxException e) {
            // Unknown error
            mErrorMsg = "Unknown error.  Try again.";
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... progress) {
    	int percent = (int) (100.0 * (double) progress[0] / mFileLen + 0.5);
		mDialog.setProgress(percent);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        if (result) {
            // play downloaded or selected recorded file
        	mPlayer = new MediaPlayer();
        	Toast.makeText(mContext, "Now playing", Toast.LENGTH_SHORT).show();
        	try {
        		File file = new File(Environment.getExternalStorageDirectory() + "/Bey2ollakRecord");
        		long size = dirSize(file);
				mPlayer.setDataSource(Environment.getExternalStorageDirectory() + "/Bey2ollakRecord" + "/" + fileName);
				mPlayer.prepare();
				mPlayer.start();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
        } else {
            // Couldn't download it, so show an error
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }
    
    private static long dirSize(File dir) {
        long result = 0;

        Stack<File> dirlist= new Stack<File>();
        dirlist.clear();

        dirlist.push(dir);

        while(!dirlist.isEmpty())
        {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();
            for(File f: fileList){
                if(f.isDirectory())
                    dirlist.push(f);
                else
                    result += f.length();
            }
        }

        return result;
    }


}
