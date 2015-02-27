package com.bey2ollaktask;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adatpter.GetDataAdapter;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

public class AudioRecord extends ActionBarActivity {

	private ImageButton startButton;
	private ImageButton stopButton;
	private ImageButton recordButton;
	private ImageButton uploadAudio;

	ProgressDialog mDialog;

	Context context;

	GetDataAdapter adapter;
	ArrayList<Entry> savedResult;

	File renameFrom, renameTo;

	ListView listView;

	Date date;
	DateFormat df;

	String audioName;
	String changedFileName;
	String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

	private Button mSubmit;
	private Button getData;
	RelativeLayout mDisplay;

	private static final String APP_KEY = "o1v5qhtfd2nbpxt";
	private static final String APP_SECRET = "c9g649r71enza01";

	private static final String ACCOUNT_PREFS_NAME = "prefs";
	private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
	private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

	DropboxAPI<AndroidAuthSession> mApi;

	private TextView timerValue;

	private long startTime = 0L;

	private Handler customHandler = new Handler();

	long timeInMilliseconds = 0L;
	long updatedTime = 0L;

	boolean mStartRecording;
	boolean mStartPlaying;

	private static final String LOG_TAG = "AudioRecord";
	private static String mFileName = null;

	private MediaRecorder mRecorder = null;

	private MediaPlayer mPlayer = null;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// Authentication to dropbox
		AndroidAuthSession session = buildSession();
		mApi = new DropboxAPI<AndroidAuthSession>(session);
		setContentView(R.layout.activity_audio_record);
		context = this;

		timerValue = (TextView) findViewById(R.id.timerValue);

		startButton = (ImageButton) findViewById(R.id.playButton);
		stopButton = (ImageButton) findViewById(R.id.stopButton);
		recordButton = (ImageButton) findViewById(R.id.recordButton);
		uploadAudio = (ImageButton) findViewById(R.id.uploadButton);

		listView = (ListView) findViewById(R.id.listView1);

		startButton.setVisibility(View.INVISIBLE);

		mSubmit = (Button) findViewById(R.id.auth_button);

		mDisplay = (RelativeLayout) findViewById(R.id.layout);
		//Handling screen rotation without losing data
		//get saved states before rotation
		if (icicle != null && (icicle.getSerializable("array") != null)) {
			savedResult = (ArrayList<Entry>) icicle.getSerializable("array");
			adapter = new GetDataAdapter(context, R.id.listView1, savedResult);
			if (savedResult.isEmpty()) {
				showToast("There is no uploaded record");
				listView.setAdapter(null);
			} else {

				listView.setAdapter(adapter);
				listView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {

						Entry obj = (Entry) listView.getAdapter().getItem(
								position);
						DownloadRecordedAudio download = new DownloadRecordedAudio(
								AudioRecord.this, mApi, obj.path, obj
										.fileName());
						download.execute();

					}
				});
			}

		}

		savedResult = new ArrayList<Entry>();

		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				// Start the remote authentication
				mApi.getSession().startOAuth2Authentication(AudioRecord.this);
			}
		});

		stopButton.setEnabled(false);

		// start record voice
		recordButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				mStartRecording = true;
				recordButton.setEnabled(false);
				if (!stopButton.isEnabled())
					stopButton.setEnabled(true);

				startTime = SystemClock.uptimeMillis();
				customHandler.postDelayed(updateTimerThread, 0);
				startButton.setVisibility(View.INVISIBLE);
				stopButton.setVisibility(View.VISIBLE);

				onRecord(mStartRecording);
				if (mStartRecording) {
					stopButton.setVisibility(View.VISIBLE);
					startButton.setVisibility(View.INVISIBLE);
				} else {
					stopButton.setVisibility(View.INVISIBLE);
					startButton.setVisibility(View.VISIBLE);
				}
				mStartRecording = !mStartRecording;

			}
		});

		// stop record voice
		stopButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {

				recordButton.setEnabled(true);
				stopRecording();

				customHandler.removeCallbacks(updateTimerThread);
				stopButton.setVisibility(View.INVISIBLE);
				startButton.setVisibility(View.VISIBLE);
				// alertdialog for renaming file
				AlertDialog.Builder builder = new AlertDialog.Builder(
						AudioRecord.this);
				builder.setTitle("Enter file name");

				// Set up the input
				final EditText input = new EditText(AudioRecord.this);
				// normal text type
				input.setInputType(InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_VARIATION_NORMAL);
				builder.setView(input);
				builder.setCancelable(false);

				// Set up the buttons
				builder.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								changedFileName = input.getText().toString();
								renameFrom = new File(mFileName);
								renameTo = new File(sdPath, changedFileName
										+ ".mp3");
								renameFrom.renameTo(renameTo);
							}
						});

				builder.show();

			}
		});

		startButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mStartPlaying = true;
				recordButton.setEnabled(true);
				onPlay(mStartPlaying);
				mStartPlaying = !mStartPlaying;

			}
		});
		// start upload record to voice
		uploadAudio.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (renameTo != null) {

					File outFile = new File(renameTo.toString());

					UploadRecord upload = new UploadRecord(AudioRecord.this,
							mApi, outFile);
					upload.execute();
				} else {
					showToast("Record first");
				}
			}
		});

		getData = (Button) findViewById(R.id.getDataButton);
		getData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isNetworkAvailable(context)) {
					GetDataAsync get = new GetDataAsync(AudioRecord.this, mApi);
					get.execute();
				} else {
					showToast("Check internet connection");
				}

			}
		});

		setLoggedIn(mApi.getSession().isLinked());

	}

	public AudioRecord() {
		date = new Date();
		df = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.US);

		audioName = df.format(date) + ".mp3";
		mFileName = sdPath + "/" + audioName;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		AndroidAuthSession session = mApi.getSession();

		// activity from which session.startAuthentication() was called, so that
		// Dropbox authentication completes properly.
		if (session.authenticationSuccessful()) {
			try {
				// Mandatory call to complete the auth
				session.finishAuthentication();

				// Store it locally in our app for later use
				storeAuth(session);
				setLoggedIn(true);
			} catch (IllegalStateException e) {
				showToast("Couldn't authenticate with Dropbox:"
						+ e.getLocalizedMessage());
				Log.i("Test", "Error authenticating", e);
			}
		}
	}

	// start record voice
	private void onRecord(boolean start) {
		if (start) {
			startRecording();
		} else {
			stopRecording();
		}
	}

	// start play voice
	private void onPlay(boolean start) {
		if (start) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	// start playing voice
	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(sdPath + "/" + changedFileName + ".mp3");
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}
	}

	// stop playing voice
	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
	}

	// start recording
	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(mFileName);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setMaxDuration(30000);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}

		mRecorder.start();
	}

	// stop recording
	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
	}

	// Timer
	private Runnable updateTimerThread = new Runnable() {

		public void run() {

			timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

			updatedTime = timeInMilliseconds;

			int secs = (int) (updatedTime / 1000);
			int mins = secs / 60;
			secs = secs % 60;
			int milliseconds = (int) (updatedTime % 1000);
			if (secs == 30) {
				showToast("Maximum lenght = 30 secs");
				customHandler.removeCallbacks(updateTimerThread);
			} else {
				timerValue.setText("" + mins + ":"
						+ String.format("%02d", secs) + ":"
						+ String.format("%03d", milliseconds));
				customHandler.postDelayed(this, 0);
			}

		}

	};

	// dropbox auth session
	private AndroidAuthSession buildSession() {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

		AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
		loadAuth(session);
		return session;
	}

	// load Auth session

	private void loadAuth(AndroidAuthSession session) {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key == null || secret == null || key.length() == 0
				|| secret.length() == 0)
			return;

		if (key.equals("oauth2:")) {
			// If the key is set to "oauth2:", then we can assume the token is
			// for OAuth 2.
			session.setOAuth2AccessToken(secret);
		} else {
			// Still support using old OAuth 1 tokens.
			session.setAccessTokenPair(new AccessTokenPair(key, secret));
		}
	}

	private void setLoggedIn(boolean loggedIn) {
		if (loggedIn) {
			mSubmit.setVisibility(View.GONE);
			mDisplay.setVisibility(View.VISIBLE);
		} else {
			mSubmit.setText("Link with Dropbox");
			mDisplay.setVisibility(View.GONE);
		}
	}

	private void storeAuth(AndroidAuthSession session) {
		// Store the OAuth 2 access token, if there is one.
		String oauth2AccessToken = session.getOAuth2AccessToken();
		if (oauth2AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME,
					0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, "oauth2:");
			edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
			edit.commit();
			return;
		}
		// Store the OAuth 1 access token, if there is one. This is only
		// necessary if
		// you're still using OAuth 1.
		AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
		if (oauth1AccessToken != null) {
			SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME,
					0);
			Editor edit = prefs.edit();
			edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
			edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
			edit.commit();
			return;
		}
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		error.show();
	}

	// async class to get saved records from my dropbox
	public class GetDataAsync extends AsyncTask<Void, Void, ArrayList<Entry>> {

		private DropboxAPI<?> mApi;
		Context ctx;

		public GetDataAsync(Context ctx, DropboxAPI<?> mApi) {
			this.ctx = ctx;
			this.mApi = mApi;
			mDialog = new ProgressDialog(context);
			mDialog.setMessage("Loading ..");
			mDialog.show();

		}

		@Override
		protected ArrayList<Entry> doInBackground(Void... params) {
			Entry dirent = null;
			try {
				dirent = mApi.metadata("/", 1000, null, true, null);
			} catch (DropboxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ArrayList<Entry> files = new ArrayList<Entry>();
			for (Entry ent : dirent.contents)
				files.add(ent);// Add it to the list

			return files;
		}

		@Override
		protected void onPostExecute(ArrayList<Entry> result) {
			super.onPostExecute(result);
			mDialog.dismiss();
			savedResult = result;
			adapter = new GetDataAdapter(ctx, R.id.listView1, result);
			if (result.isEmpty()) {
				showToast("There is no uploaded record");
				listView.setAdapter(null);
			} else {

				listView.setAdapter(adapter);
				listView.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {

						Entry obj = (Entry) listView.getAdapter().getItem(
								position);
						DownloadRecordedAudio download = new DownloadRecordedAudio(
								AudioRecord.this, mApi, obj.path, obj
										.fileName());
						download.execute();

					}
				});
			}
		}
	}

	public static boolean isNetworkAvailable(Context ctx) {
		return ((ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo() != null;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		outState.putSerializable("array", savedResult);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		savedResult = (ArrayList<Entry>) savedInstanceState
				.getSerializable("array");
	}

}
