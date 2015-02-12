package com.adatpter;

import java.util.ArrayList;

import com.bey2ollaktask.R;
import com.model.GetDataModel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class GetDataAdapter extends ArrayAdapter<GetDataModel> {

	ArrayList<GetDataModel> val;
	Context ctx;
	int pos;
	ViewHolder holder;

	public GetDataAdapter(Context ctx, int resource, ArrayList<GetDataModel> val) {
		super(ctx, resource, val);
		this.ctx = ctx;
		this.val = val;
	}

	static class ViewHolder {
		public TextView Name;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		pos = position;
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.items, parent, false);
			holder = new ViewHolder();
			holder.Name = (TextView) convertView
					.findViewById(R.id.itemTextView);
			convertView.setTag(holder);

		} else {
			holder = (ViewHolder) convertView.getTag();

		}
		holder.Name.setText(val.get(position).getName());
		return convertView;

	}

}
