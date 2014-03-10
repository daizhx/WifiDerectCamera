package com.daizhx.wifiderectcamera;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

public class FilesActivity extends ListActivity {
	
	private static String filesRootPath = null;
	private ArrayList<String> fnames = null;
	private ArrayList<String> fpaths = null;
	ListAdapter adapter = null;
	private View view;
	private EditText editText; 
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		Log.d("daizhx", "click");
		String path = fpaths.get(position);
		File file = new File(path);
		openFile(file);
		super.onListItemClick(l, v, position, id);
	}
	
	private void openFile(File file){
		if(file.exists() && file.canRead()){
			Intent intent = new Intent();
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setAction(android.content.Intent.ACTION_VIEW);
			
			String type = getMIMEType(file);
			intent.setDataAndType(Uri.fromFile(file), type);
			startActivity(intent);
		}
	}
	
	private String getMIMEType(File file) {
		// TODO Auto-generated method stub
		String type = null;
		String name = file.getName();
		String end = name.substring(name.lastIndexOf(".")+1, name.length()).toLowerCase();
		 if (end.equals("m4a") || end.equals("mp3") || end.equals("wav")){
	            type = "audio";
	        }
	        else if(end.equals("mp4") || end.equals("3gp")) {
	            type = "video";
	        }
	        else if (end.equals("jpg") || end.equals("png") || end.equals("jpeg") || end.equals("bmp") || end.equals("gif")){
	            type = "image";
	        }
	        else {
	            //如果无法直接打开，跳出列表由用户选择
	            type = "*";
	        }
	        type += "/*";
	        return type;

	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		FileUtil fileUtil = new FileUtil();
		filesRootPath = fileUtil.getSDCardRoot()+"ipcamera";
		File file = new File(filesRootPath);
		showFiles(file);
		ListView lv = getListView();
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				String path = fpaths.get(arg2);
				File file = new File(path);
				fileHandle(file);
				
				Log.d("daizhx", "long click:arg0="+arg0+",arg1="+arg1+",arg2="+arg2+",arg3="+arg3);
				return false;
			}
			
		});
	}	
	private void showFiles(File dir){
		fnames = new ArrayList<String>();
		fpaths = new ArrayList<String>();
		if(dir.exists() && dir.isDirectory()){
			File[] files = dir.listFiles();
			for(File f : files){
				fnames.add(f.getName());
				fpaths.add(f.getPath());
				Log.d("daizhx", "files:"+f.getName()+","+f.getPath());
			}
		}
		adapter = new ArrayAdapter(this,R.layout.listviewitem, R.id.listItem, fnames);
		setListAdapter(adapter);
	}
	 private void fileHandle(final File file){
	        OnClickListener listener = new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	                //
	                if (which == 0){
	                    openFile(file);
	                }
	                //
	                else if(which == 1){
	                    LayoutInflater factory = LayoutInflater.from(FilesActivity.this);
	                    view = factory.inflate(R.layout.rename_dialog, null);
	                    editText = (EditText)view.findViewById(R.id.editText);
	                    editText.setText(file.getName());
	                    
	                    OnClickListener listener2 = new DialogInterface.OnClickListener() {
	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                            // TODO Auto-generated method stub
	                            String modifyName = editText.getText().toString();
	                            final String fpath = file.getParentFile().getPath();
	                            final File newFile = new File(fpath + "/" + modifyName);
	                            if (newFile.exists()){
	                                //
	                                if (!modifyName.equals(file.getName())){
	                                    new AlertDialog.Builder(FilesActivity.this)
	                                    .setTitle("note!")
	                                    .setMessage("filename has exist! want override?")
	                                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
	                                        @Override
	                                        public void onClick(DialogInterface dialog, int which) {
	                                            if (file.renameTo(newFile)){
	                                            	showFiles(new File(fpath));
	                                            }
	                                            else{
	                                            	
	                                            }
	                                        }
	                                    })
	                                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
	                                        @Override
	                                        public void onClick(DialogInterface dialog, int which) {
	                                            
	                                        }
	                                    })
	                                    .show();
	                                }
	                            }
	                            else{
	                                if (file.renameTo(newFile)){
	                                    showFiles(new File(fpath));
	                                }
	                                else{
	                                    
	                                }
	                            }
	                        }
	                    };
	                    AlertDialog renameDialog = new AlertDialog.Builder(FilesActivity.this).create();
	                    renameDialog.setView(view);
	                    renameDialog.setButton("yes", listener2);
	                    renameDialog.setButton2("cancel", new DialogInterface.OnClickListener() {
	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                            // TODO Auto-generated method stub
	                            
	                        }
	                    });
	                    renameDialog.show();
	                }
	                //
	                else{
	                    new AlertDialog.Builder(FilesActivity.this)
	                    .setTitle("note!")
	                    .setMessage("delete the file?")
	                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                            if(file.delete()){
	                                //
	                                showFiles(new File(file.getParent()));
	                                
	                            }
	                            else{
	                                
	                            }
	                        }
	                    })
	                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                            
	                        }
	                    }).show();
	                }
	            }
	        };
	        //
	        String[] menu = {"open","rename","delete"};
	        new AlertDialog.Builder(FilesActivity.this)
	        .setItems(menu, listener)
	        .setPositiveButton("cancel", new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	                
	            }
	        }).show();
	    }
}
