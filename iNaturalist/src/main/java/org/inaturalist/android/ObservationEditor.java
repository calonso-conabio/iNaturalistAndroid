package org.inaturalist.android;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.crashlytics.android.Crashlytics;
import com.flurry.android.FlurryAgent;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jraf.android.backport.switchwidget.Switch;
import org.json.JSONArray;
import org.json.JSONException;
import org.lucasr.twowayview.TwoWayView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import com.ptashek.widgets.datetimepicker.DateTimePicker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class ObservationEditor extends SherlockFragmentActivity {
    private final static String TAG = "INAT: ObservationEditor";
    public final static String TAKE_PHOTO = "take_photo";
    public final static String CHOOSE_PHOTO = "choose_photo";
    private Uri mUri;
    private Cursor mCursor;
    private Cursor mImageCursor;
    private TextView mSpeciesGuessTextView;
    private TextView mDescriptionTextView;
    private TextView mSaveButton;
    private TextView mObservedOnStringTextView;
    private TextView mObservedOnButton;
    private TextView mTimeObservedAtButton;
    private TwoWayView mGallery;
    private TextView mLatitudeView;
    private TextView mLongitudeView;
    private TextView mAccuracyView;
    private ProgressBar mLocationProgressView;
    private View mLocationRefreshButton;
    private ImageButton mLocationStopRefreshButton;
    private View mProjectSelector;
    private Uri mFileUri;
    private Observation mObservation;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mCurrentLocation;
    private Long mLocationRequestedAt;
    private INaturalistApp app;
    private ActivityHelper mHelper;
    private boolean mCanceled = false;
    
    private ActionBar mTopActionBar;
    private ImageButton mDeleteButton;
    private ImageButton mViewOnInat;
    private TextView mObservationCommentsIds;
    private TableLayout mProjectFieldsTable;

    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int COMMENTS_IDS_REQUEST_CODE = 101;
    private static final int PROJECT_SELECTOR_REQUEST_CODE = 102;
    private static final int LOCATION_CHOOSER_REQUEST_CODE = 103;
    private static final int OBSERVATION_PHOTOS_REQUEST_CODE = 104;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int DATE_DIALOG_ID = 0;
    private static final int TIME_DIALOG_ID = 1;
    private static final int ONE_MINUTE = 60 * 1000;
    
    private static final int PROJECT_FIELD_TAXON_SEARCH_REQUEST_CODE = 301;
    private static final int TAXON_SEARCH_REQUEST_CODE = 302;
    public static final String SPECIES_GUESS = "species_guess";
	public static final String OBSERVATION_PROJECT = "observation_project";
    
    private List<ProjectFieldViewer> mProjectFieldViewers;
    private Switch mIdPlease;
    private Spinner mGeoprivacy;
    private String mSpeciesGuess;
    private TableLayout mProjectsTable;
    private ProjectReceiver mProjectReceiver;
        
    
    private ArrayList<BetterJSONObject> mProjects = null;
    
	private boolean mProjectFieldsUpdated = false;
	private boolean mDeleted = false;
	private ImageView mTaxonSelector;
    private boolean mIsConfirmation;
    private boolean mPictureTaken;
    private boolean mWatchForSpeciesGuessChanges;
    private ImageView mSpeciesGuessIcon;
    private String mPreviousTaxonSearch = "";
    private String mTaxonPicUrl;
    private boolean mIsTaxonUnknown;

    @Override
	protected void onStart()
	{
		super.onStart();
		FlurryAgent.onStartSession(this, INaturalistApp.getAppContext().getString(R.string.flurry_api_key));
		FlurryAgent.logEvent(this.getClass().getSimpleName());
	}

	@Override
	protected void onStop()
	{
		super.onStop();		
		FlurryAgent.onEndSession(this);
	}	

    private class ProjectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mProjects = new ArrayList<BetterJSONObject>();
        	SerializableJSONArray serializableArray = (SerializableJSONArray) intent.getSerializableExtra(INaturalistService.PROJECTS_RESULT);
            JSONArray projectList = new JSONArray();
        	
        	if (serializableArray != null) {
        		projectList = serializableArray.getJSONArray();
        	}

            for (int i = 0; i < projectList.length(); i++) {
                try {
                    mProjects.add(new BetterJSONObject(projectList.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Collections.sort(mProjects, new Comparator<BetterJSONObject>() {
                @Override
                public int compare(BetterJSONObject lhs, BetterJSONObject rhs) {
                    return lhs.getString("title").compareTo(rhs.getString("title"));
                }
            });

            refreshProjectList();
        }
    }

    private void refreshProjectList() {
        if (!mIsConfirmation) {
            mProjectsTable.removeAllViews();
        }

        if (mProjects == null) {
            return;
        }

        for (BetterJSONObject project : mProjects) {
            Integer projectId = project.getInt("id");
            
            if (projectId == null) continue;

            if (mProjectIds.contains(Integer.valueOf(projectId))) {
                // Observation was added to current project
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.project_selector_item, mProjectsTable, false); 
                BetterJSONObject item = project;

                TextView projectName = (TextView) view.findViewById(R.id.project_name);
                projectName.setText(item.getString("title"));
                TextView projectDescription = (TextView) view.findViewById(R.id.project_description);
                // Strip HTML tags
                String noHTML = Html.fromHtml(item.getString("description")).toString();
                projectDescription.setText(noHTML);
                ImageView userPic = (ImageView) view.findViewById(R.id.project_pic);
                UrlImageViewHelper.setUrlDrawable(userPic, item.getString("icon_url"));

                ImageView projectSelected = (ImageView) view.findViewById(R.id.project_selected);
                projectSelected.setVisibility(View.GONE);
                
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) projectName.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                projectName.setLayoutParams(params);
                
                mProjectsTable.addView(view);
            }
        }
    }


    
    private class ProjectFieldViewer {
        private ProjectField mField;
        private ProjectFieldValue mFieldValue;
        
        private int mTaxonId = -1;
        
        // UI elements
        private TextView mFieldName;
        private EditText mEditText;
        private Spinner mSpinner;
        private RelativeLayout mDateContainer;
        private ImageView mSetDate;
        private TextView mDate;
        private RelativeLayout mTaxonContainer;
        private ImageView mTaxonPic;
        private TextView mIdName;
        private TextView mIdTaxonName;
        private ArrayAdapter<String> mSpinnerAdapter;
        private TaxonReceiver mTaxonReceiver;
        private TextView mFieldDescription;
        
        @SuppressLint("ValidFragment")
		private class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
            
            private boolean mIsCanceled;
            private int mHour, mMinute;
            
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                String currentHour = mDate.getText().toString();
                Calendar c;
                
                mIsCanceled = false;
                
                if (currentHour.equals("")) {
                    // Use the current hour as the default hour in the picker
                    c = Calendar.getInstance();
                } else {
                    Date date;
                    c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(currentHour.split(":")[0]));
                    c.set(Calendar.MINUTE, Integer.valueOf(currentHour.split(":")[1]));
                }
                
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);

                // Create a new instance of TimePickerDialog and return it
                TimePickerDialog dialog = new TimePickerDialog(getActivity(), this, hour, minute, true);
                dialog.setOnCancelListener(this);
                dialog.setOnDismissListener(this);
                
                return dialog;
            }
            
            public void onCancel(DialogInterface dialog) {
                mIsCanceled = true;
            }
            
            public void onDismiss (DialogInterface dialog) {
                if (!mIsCanceled) {
                    mDate.setText(String.format("%02d:%02d", mHour, mMinute));
                }
                
                dismiss();
            }

            @Override
            public void onTimeSet(TimePicker view, int hour, int minute) {
                mHour = hour;
                mMinute = minute;
            }
        }        
    

        @SuppressLint("ValidFragment")
		private class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
            
            private boolean mIsCanceled;
            private int mYear, mMonth, mDay;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                String currentDate = mDate.getText().toString();
                Calendar c;
                
                mIsCanceled = false;
                
                if (currentDate.equals("")) {
                    // Use the current date as the default date in the picker
                    c = Calendar.getInstance();
                } else {
                    Date date;
                    c = Calendar.getInstance();
                    try {
                        date = new SimpleDateFormat("yyyy-MM-dd").parse(currentDate);
                        c.setTime(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                // Create a new instance of DatePickerDialog and return it
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
                dialog.setOnCancelListener(this);
                dialog.setOnDismissListener(this);
                
                return dialog;
            }
            
            public void onCancel(DialogInterface dialog) {
                mIsCanceled = true;
            }
            
            public void onDismiss (DialogInterface dialog) {
                if (!mIsCanceled) {
                    mDate.setText(String.format("%d-%02d-%02d", mYear, mMonth + 1, mDay));
                }
                
                dismiss();
            }

            public void onDateSet(DatePicker view, int year, int month, int day) {
                mYear = year;
                mMonth = month;
                mDay = day;
            }
        }
        
     
        private class TaxonReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(mTaxonReceiver);
                
                BetterJSONObject taxon = (BetterJSONObject) intent.getSerializableExtra(INaturalistService.TAXON_RESULT);
                
                if (taxon == null) {
                    return;
                }
                
                int taxonId = taxon.getInt("id");
                
                if (taxonId != mTaxonId) {
                    // Result was not from out taxon
                    return;
                }

                UrlImageViewHelper.setUrlDrawable(mTaxonPic, taxon.getString("image_url"));
                mIdName.setText(taxon.getString("unique_name"));
                mIdTaxonName.setText(taxon.getString("name"));
                mIdTaxonName.setTypeface(null, Typeface.ITALIC);
            }
        }

        
        public ProjectFieldViewer(ProjectField field, ProjectFieldValue fieldValue) {
            mField = field;
            mFieldValue = fieldValue;
            
            if (mFieldValue == null) {
                mFieldValue = new ProjectFieldValue();
            }
            
            if (mFieldValue.value == null) {
                mFieldValue.value = "";
            }
        }
        
        public ProjectField getField() {
            return mField;
        }
        
        public String getValue() {
            if ((mField.data_type.equals("text")) && (mField.allowed_values != null) && (!mField.allowed_values.equals(""))) {
                return (String) mSpinner.getSelectedItem();
            } else if (mField.data_type.equals("text")) {
                return mEditText.getText().toString();
            } else if (mField.data_type.equals("numeric")) {
                String val = mEditText.getText().toString();
                return (val.equals("") ? null : val);
            } else if (mField.data_type.equals("date")) {
                String dateString = mDate.getText().toString();
                if (dateString.equals("")) return null;
                return dateString;
            } else if (mField.data_type.equals("time")) {
                String dateString = mDate.getText().toString();
                if (dateString.equals("")) return null;
                return dateString;
            } else if (mField.data_type.equals("datetime")) {
                String dateString = mDate.getText().toString();
                if (dateString.equals("")) return null;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateString);
                    
                    // Return a ISO8601 date string
                    String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
                    return formatted.substring(0, 22) + ":" + formatted.substring(22); 

                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
            } else if (mField.data_type.equals("taxon")) {
                if (mTaxonId == -1) return null;
                return String.valueOf(mTaxonId);
            } else {
                return null;
            }
            
        }
        
        public Boolean isValid() {
            if (mField.is_required) {
                String value = getValue();
                if (value == null || value.equals("")) {
                    // Mandatory field
                    return false;
                }
            }
            
            if ((mField.data_type.equals("numeric")) && (!mEditText.getText().toString().equals("")))  {
                try {
                    float value = Float.valueOf(mEditText.getText().toString());
                } catch (Exception exc) {
                    // Invalid number
                    return false;
                }
            }


            return true;
        }
        
        public void onTaxonSearchResult(Intent data) {
            int taxonId = data.getIntExtra(TaxonSearchActivity.TAXON_ID, 0);
            String taxonName = data.getStringExtra(TaxonSearchActivity.TAXON_NAME);
            String idName = data.getStringExtra(TaxonSearchActivity.ID_NAME);
            String idImageUrl = data.getStringExtra(TaxonSearchActivity.ID_PIC_URL);
            int fieldId = data.getIntExtra(TaxonSearchActivity.FIELD_ID, 0);
            
            if ((fieldId != mField.field_id.intValue()) || (!mField.data_type.equals("taxon"))) {
                // Not our field
                return;
            }
            
            mTaxonId = taxonId;
            UrlImageViewHelper.setUrlDrawable(mTaxonPic, idImageUrl);
            mIdName.setText(idName);
            mIdTaxonName.setText(taxonName);
            mIdTaxonName.setTypeface(null, Typeface.ITALIC);
        }
        
        public View getView() {
            TableRow row = (TableRow) LayoutInflater.from(ObservationEditor.this).inflate(R.layout.project_field, null);
            row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            
            mFieldName = (TextView) row.findViewById(R.id.field_name);
            mFieldDescription = (TextView) row.findViewById(R.id.field_description);
            mEditText = (EditText) row.findViewById(R.id.edit_text);
            mSpinner = (Spinner) row.findViewById(R.id.spinner);
            mDateContainer = (RelativeLayout) row.findViewById(R.id.date_container);
            mSetDate = (ImageView) row.findViewById(R.id.set_date);
            mDate = (TextView) row.findViewById(R.id.date);
            mTaxonContainer = (RelativeLayout) row.findViewById(R.id.taxon_container);
            mTaxonPic = (ImageView) row.findViewById(R.id.taxon_pic);
            mIdName = (TextView) row.findViewById(R.id.id_name);
            mIdTaxonName = (TextView) row.findViewById(R.id.id_taxon_name);
            mIdTaxonName.setTypeface(null, Typeface.ITALIC);
            
            mFieldName.setText(mField.name);
            mFieldDescription.setText(mField.description);
            
            if ((mField.description == null) || (mField.description.length() == 0)) {
                mFieldDescription.setVisibility(View.GONE);
            } else {
                mFieldDescription.setVisibility(View.VISIBLE);
            }
            
            if (mField.is_required) {
                mFieldName.setTypeface(null, Typeface.BOLD);
            }
            
            if ((mField.data_type.equals("text")) && (mField.allowed_values != null) && (!mField.allowed_values.equals(""))) {
                mSpinner.setVisibility(View.VISIBLE);
                String[] allowedValues = mField.allowed_values.split("\\|");
                mSpinnerAdapter = new ArrayAdapter<String>(ObservationEditor.this, android.R.layout.simple_spinner_item, android.R.id.text1, allowedValues);
                mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinner.setAdapter(mSpinnerAdapter);
                
                int position = mSpinnerAdapter.getPosition(mFieldValue.value);
                if (position != -1) {
                    mSpinner.setSelection(position);
                }
            } else if (mField.data_type.equals("text")) {
                mEditText.setVisibility(View.VISIBLE);
                mEditText.setText(mFieldValue.value);
            } else if (mField.data_type.equals("numeric")) {
                mEditText.setVisibility(View.VISIBLE);
                mEditText.setText(mFieldValue.value);
                mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);
            } else if (mField.data_type.equals("date")) {
                mDateContainer.setVisibility(View.VISIBLE);
                
                if (!mFieldValue.value.equals("")) {
                    mDate.setText(formatDate(mFieldValue.value));
                } else {
                    mDate.setText("");
                }
                mDateContainer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogFragment newFragment = new DatePickerFragment();
                        newFragment.show(ObservationEditor.this.getSupportFragmentManager(), "datePicker");
                    }
                });
                
            } else if (mField.data_type.equals("time")) {
                mDateContainer.setVisibility(View.VISIBLE);
                mDate.setText(mFieldValue.value);
                mSetDate.setImageResource(R.drawable.ic_action_time);
                mDateContainer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogFragment newFragment = new TimePickerFragment();
                        newFragment.show(ObservationEditor.this.getSupportFragmentManager(), "timePicker");
                    }
                });
                
                
            } else if (mField.data_type.equals("datetime")) {
                mDateContainer.setVisibility(View.VISIBLE);
                // date time = 2013-11-14T13:23:37+00:00
                if (!mFieldValue.value.equals("")) {
                    mDate.setText(formatDateTime(mFieldValue.value));
                } else {
                    mDate.setText("");
                }
                mDateContainer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showDateTimeDialog();
                    }
                });
                
            } else if (mField.data_type.equals("taxon")) {
                mTaxonContainer.setVisibility(View.VISIBLE);
                
                if (mTaxonId == -1) {
                    mTaxonId = (mFieldValue.value.equals("") ? -1 : Integer.valueOf(mFieldValue.value));
                }
                
                if (mTaxonId != -1) {
                    // Get the taxon details
                    mTaxonReceiver = new TaxonReceiver();
                    IntentFilter filter = new IntentFilter(INaturalistService.ACTION_GET_TAXON_RESULT);
                    Log.i(TAG, "Registering ACTION_GET_TAXON_RESULT");
                    registerReceiver(mTaxonReceiver, filter);  
 
                    Intent serviceIntent = new Intent(INaturalistService.ACTION_GET_TAXON, null, ObservationEditor.this, INaturalistService.class);
                    serviceIntent.putExtra(INaturalistService.TAXON_ID, mTaxonId);
                    startService(serviceIntent);
                } else {
                    mIdName.setText("");
                    mIdTaxonName.setText("");
                }
                mTaxonContainer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ObservationEditor.this, TaxonSearchActivity.class);
                        intent.putExtra(TaxonSearchActivity.FIELD_ID, mField.field_id);
                        startActivityForResult(intent, PROJECT_FIELD_TAXON_SEARCH_REQUEST_CODE);
                    }
                });

            }
            
            return row;
        }
        
        private void showDateTimeDialog() {
            // Create the dialog
            final Dialog mDateTimeDialog = new Dialog(ObservationEditor.this);
            // Inflate the root layout
            final RelativeLayout mDateTimeDialogView = (RelativeLayout) getLayoutInflater().inflate(R.layout.date_time_dialog, null);
            // Grab widget instance
            final DateTimePicker mDateTimePicker = (DateTimePicker) mDateTimeDialogView.findViewById(R.id.DateTimePicker);

            // When the "OK" button is clicked 
            ((Button) mDateTimeDialogView.findViewById(R.id.SetDateTime)).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mDateTimePicker.clearFocus();
                    
                    Date selectedTime = mDateTimePicker.getDate();
                    String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(selectedTime);
                    mDate.setText(formatted);
                    
                    mDateTimeDialog.dismiss();
                }
            });

            // Cancel the dialog when the "Cancel" button is clicked
            ((Button) mDateTimeDialogView.findViewById(R.id.CancelDialog)).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mDateTimeDialog.cancel();
                }
            });

            // Reset Date and Time pickers when the "Reset" button is clicked
            ((Button) mDateTimeDialogView.findViewById(R.id.ResetDateTime)).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mDateTimePicker.reset();
                }
            });

           
            // Setup TimePicker
            mDateTimePicker.setIs24HourView(true);
            // No title on the dialog window
            mDateTimeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            // Set the dialog content view
            mDateTimeDialog.setContentView(mDateTimeDialogView);
            
            String dateValue = mDate.getText().toString();

            if (!dateValue.equals("")) {
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dateValue);
                    mDateTimePicker.updateDate(date.getYear() + 1900, date.getMonth(), date.getDate());
                    mDateTimePicker.updateTime(date.getHours(), date.getMinutes());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            
            // Display the dialog
            mDateTimeDialog.show();  
        }
        
        private Date valueToDateTime(String value) {
            Calendar calendar = GregorianCalendar.getInstance();
            String s = value.replace("Z", "+00:00");
            try {
                s = s.substring(0, 22) + s.substring(23);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                return null;
            }
            Date date;
            try {
                date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            
            calendar.setTime(date);
            return calendar.getTime();
        }

        private Date valueToDate(String value) {
            Calendar calendar = GregorianCalendar.getInstance();
            Date date;
            try {
                date = new SimpleDateFormat("yyyy-MM-dd").parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            
            calendar.setTime(date); 
            
            return calendar.getTime();
        }
 
        
        private String formatDate(String value) {
            Date date = valueToDate(value);
            String formatted = new SimpleDateFormat("yyyy-MM-dd").format(date);
            return formatted;
        }
        
        private String formatDateTime(String value) {
            Date date = valueToDateTime(value);
            String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
            return formatted;
        }
 
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsConfirmation) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.observation_confirmation_menu, menu);
        }

        return true;
    }

    /**
     * LIFECYCLE CALLBACKS
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        if ((savedInstanceState == null) && (intent != null) && (intent.getData() != null)) {
            int uriMatch = ObservationProvider.URI_MATCHER.match(intent.getData());
            if ((uriMatch == Observation.OBSERVATIONS_URI_CODE) || (uriMatch == ObservationPhoto.OBSERVATION_PHOTOS_URI_CODE)) {
                // Show the confirmation screen
                mIsConfirmation = true;
            } else {
                mIsConfirmation = false;
            }
        } else {
            // Show the observation editor screen
            mIsConfirmation = savedInstanceState.getBoolean("mIsConfirmation", false);
        }

        mIsConfirmation = false;

        if (mIsConfirmation) {
            setContentView(R.layout.observation_confirmation);
            setTitle(R.string.details);
        } else {
            setContentView(R.layout.observation_editor);
        }

        if (app == null) {
            app = (INaturalistApp) getApplicationContext();
        }
        if (mHelper == null) {
            mHelper = new ActivityHelper(this);
        }


        if (savedInstanceState == null) {
            // Do some setup based on the action being performed.
            Uri uri = intent.getData();
            if (uri == null) {
                Log.e(TAG, "Null URI from intent.getData");
                finish();
                return;
            }
            switch (ObservationProvider.URI_MATCHER.match(uri)) {
            case Observation.OBSERVATION_ID_URI_CODE:
                getIntent().setAction(Intent.ACTION_EDIT);
                mUri = uri;
                break;
            case Observation.OBSERVATIONS_URI_CODE:
                mUri = getContentResolver().insert(uri, null);
                if (mUri == null) {
                    Log.e(TAG, "Failed to insert new observation into " + uri);
                    finish();
                    return;
                }
                setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
                getIntent().setAction(Intent.ACTION_INSERT);
                break;
            case ObservationPhoto.OBSERVATION_PHOTOS_URI_CODE:
                mFileUri = (Uri) intent.getExtras().get("photoUri");
                if (mFileUri == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.photo_not_specified), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                mFileUri = getPath(this, mFileUri);
                mUri = getContentResolver().insert(Observation.CONTENT_URI, null);
                if (mUri == null) {
                    Log.e(TAG, "Failed to insert new observation into " + uri);
                    finish();
                    return;
                }
                mCursor = managedQuery(mUri, Observation.PROJECTION, null, null, null);
                mObservation = new Observation(mCursor);
                updateImageOrientation(mFileUri);
                createObservationPhotoForPhoto(mFileUri);
                setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
                getIntent().setAction(Intent.ACTION_INSERT);
                mFileUri = null;
                break;
            default:
                Log.e(TAG, "Unknown action, exiting");
                finish();
                return;
            }
        } else {
            String fileUri = savedInstanceState.getString("mFileUri");
            if (fileUri != null) {mFileUri = Uri.parse(fileUri);}
            String obsUri = savedInstanceState.getString("mUri");
            if (obsUri != null) {
                mUri = Uri.parse(obsUri);
            } else {
                mUri = intent.getData();
            }

            mObservation = (Observation) savedInstanceState.getSerializable("mObservation");
            mProjects = (ArrayList<BetterJSONObject>) savedInstanceState.getSerializable("mProjects");
            mProjectIds = savedInstanceState.getIntegerArrayList("mProjectIds");
            mProjectFieldValues = (HashMap<Integer, ProjectFieldValue>) savedInstanceState.getSerializable("mProjectFieldValues");
            mProjectFieldsUpdated = savedInstanceState.getBoolean("mProjectFieldsUpdated");
            mPictureTaken = savedInstanceState.getBoolean("mPictureTaken", false);
            mPreviousTaxonSearch = savedInstanceState.getString("mPreviousTaxonSearch");
            mTaxonPicUrl = savedInstanceState.getString("mTaxonPicUrl");
        }


        mTaxonSelector = (ImageView) findViewById(R.id.taxonSelector);

        if (!mIsConfirmation) {
            mTaxonSelector.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (!isNetworkAvailable()) {
                        Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(ObservationEditor.this, TaxonSearchActivity.class);
                    intent.putExtra(TaxonSearchActivity.SPECIES_GUESS, mSpeciesGuessTextView.getText().toString());
                    startActivityForResult(intent, TAXON_SEARCH_REQUEST_CODE);
                }
            });
        } else {
            findViewById(R.id.locationVisibility).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mHelper.selection(getString(R.string.location_visibility), getResources().getStringArray(R.array.geoprivacy_items), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mGeoprivacy.setSelection(which);
                            updateObservationVisibilityDescription();
                        }
                    });
                }
            });
        }

        mIdPlease = (Switch) findViewById(R.id.id_please);
        mGeoprivacy = (Spinner) findViewById(R.id.geoprivacy);
        mSpeciesGuessTextView = (TextView) findViewById(R.id.speciesGuess);
        mSpeciesGuessIcon = (ImageView) findViewById(R.id.species_guess_icon);
        mDescriptionTextView = (TextView) findViewById(R.id.description);
        mSaveButton = (TextView) findViewById(R.id.save_observation);
        mObservedOnButton = (TextView) findViewById(R.id.observed_on);
        mObservedOnStringTextView = (TextView) findViewById(R.id.observed_on_string);
        mTimeObservedAtButton = (TextView) findViewById(R.id.time_observed_at);
        mGallery = (TwoWayView) findViewById(R.id.gallery);
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        mGallery.setItemMargin((int)px);
        mLatitudeView = (TextView) findViewById(R.id.latitude);
        mLongitudeView = (TextView) findViewById(R.id.longitude);
        mAccuracyView = (TextView) findViewById(R.id.accuracy);
        mLocationProgressView = (ProgressBar) findViewById(R.id.locationProgress);
        mLocationRefreshButton = (View) findViewById(R.id.locationRefreshButton);
        mLocationStopRefreshButton = (ImageButton) findViewById(R.id.locationStopRefreshButton);
        mTopActionBar = getSupportActionBar();
        mDeleteButton = (ImageButton) findViewById(R.id.delete_observation);
        mViewOnInat = (ImageButton) findViewById(R.id.view_on_inat);
        mObservationCommentsIds = (TextView) findViewById(R.id.commentIdCount);
        mProjectSelector = findViewById(R.id.select_projects);
        mProjectFieldsTable = (TableLayout) findViewById(R.id.project_fields);
        mProjectsTable = (TableLayout) findViewById(R.id.projects);

        mProjectSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ObservationEditor.this, ProjectSelectorActivity.class);
                intent.putExtra(INaturalistService.OBSERVATION_ID, (mObservation.id == null ? mObservation._id : mObservation.id));
                intent.putIntegerArrayListExtra(INaturalistService.PROJECT_ID, mProjectIds);
                startActivityForResult(intent, PROJECT_SELECTOR_REQUEST_CODE);
            }
        });

        if (mIsConfirmation) {
            findViewById(R.id.coordinates).setVisibility(View.GONE);
            if (mTaxonPicUrl != null) {
                UrlImageViewHelper.setUrlDrawable(mSpeciesGuessIcon, mTaxonPicUrl, R.drawable.ic_species_guess_black_24dp, new UrlImageViewCallback() {
                    @Override
                    public void onLoaded(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                            mSpeciesGuessIcon.setAlpha(1.0f);
                        }
                    }

                    @Override
                    public Bitmap onPreSetBitmap(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
                        return loadedBitmap;
                    }
                });
            }

            mWatchForSpeciesGuessChanges = true;
            mSpeciesGuessTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {  }

                @Override
                public void afterTextChanged(Editable editable) {
                    String currentTaxonGuess = mSpeciesGuessTextView.getText().toString();

                    if ((mWatchForSpeciesGuessChanges) && (!currentTaxonGuess.equals(mPreviousTaxonSearch)) &&
                            (!currentTaxonGuess.equals("Unknown")) && (!currentTaxonGuess.equals(""))) {
                        Intent intent = new Intent(ObservationEditor.this, TaxonSearchActivity.class);
                        intent.putExtra(TaxonSearchActivity.SPECIES_GUESS, currentTaxonGuess);
                        intent.putExtra(TaxonSearchActivity.SHOW_UNKNOWN, true);
                        startActivityForResult(intent, TAXON_SEARCH_REQUEST_CODE);
                        mWatchForSpeciesGuessChanges = false;
                        mPreviousTaxonSearch = currentTaxonGuess;
                    }
                }
            });
        }
        
        mTopActionBar.setHomeButtonEnabled(true);
        mTopActionBar.setDisplayHomeAsUpEnabled(true);
        mTopActionBar.setIcon(android.R.color.transparent);

        View takePhoto;

        if (mIsConfirmation) {
            mTopActionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.inatapptheme_color)));
            mTopActionBar.setLogo(R.drawable.ic_arrow_back_white_24dp);
            mTopActionBar.setDisplayHomeAsUpEnabled(false);
            mTopActionBar.setTitle(Html.fromHtml("<font color=\"#ffffff\">" + getString(R.string.details) + "</font>"));
            takePhoto = findViewById(R.id.take_photo);
        } else {
            mTopActionBar.setDisplayShowCustomEnabled(true);
            mTopActionBar.setCustomView(R.layout.observation_editor_top_action_bar);
            mTopActionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_background));
            mTopActionBar.setLogo(R.drawable.up_icon);
            takePhoto = mTopActionBar.getCustomView().findViewById(R.id.take_photo);
        }

        takePhoto.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
                mFileUri = getPath(ObservationEditor.this, mFileUri);
                openImageIntent(ObservationEditor.this, mFileUri, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        if (!mIsConfirmation) {
            mObservationCommentsIds.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isNetworkAvailable()) {
                        Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(ObservationEditor.this, CommentsIdsActivity.class);
                    intent.putExtra(INaturalistService.OBSERVATION_ID, mObservation.id);
                    intent.putExtra(INaturalistService.TAXON_ID, mObservation.taxon_id);
                    startActivityForResult(intent, COMMENTS_IDS_REQUEST_CODE);

                    // Get the observation's IDs/comments
                    Intent serviceIntent = new Intent(INaturalistService.ACTION_GET_OBSERVATION, null, ObservationEditor.this, INaturalistService.class);
                    serviceIntent.putExtra(INaturalistService.OBSERVATION_ID, mObservation.id);
                    startService(serviceIntent);

                }
            });

            mViewOnInat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Display a confirmation dialog
                    confirm(ObservationEditor.this, R.string.edit_observation, R.string.view_on_inat_confirmation,
                            R.string.yes, R.string.no,
                            new Runnable() {
                                public void run() {
                                    String inatNetwork = app.getInaturalistNetworkMember();
                                    String inatHost = app.getStringResourceByName("inat_host_" + inatNetwork);

                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse("http://" + inatHost + "/observations/" + mObservation.id));
                                    startActivity(i);
                                }
                            },
                            null);
                }
            });
        }
        
        mSpeciesGuess = intent.getStringExtra(SPECIES_GUESS);

        if (!mIsConfirmation) {
            registerForContextMenu(mGallery);
        }

        initUi();

        if (!mIsConfirmation) {
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Display a confirmation dialog
                    confirm(ObservationEditor.this, R.string.edit_observation, R.string.delete_confirmation,
                            R.string.yes, R.string.no,
                            new Runnable() {
                                public void run() {
                                    delete((mObservation == null) || (mObservation.id == null));
                                    Toast.makeText(ObservationEditor.this, R.string.observation_deleted, Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            },
                            null);
                }
            });

            mSaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uiToProjectFieldValues();
                    if (save()) {
                        finish();
                    }
                }
            });

        }

        mObservedOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        mTimeObservedAtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });


        mLocationRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ObservationEditor.this);
                // Set the adapter
                String[] items = {
                        getResources().getString(R.string.get_current_location),
                        getResources().getString(R.string.edit_location)
                };
                builder.setAdapter(
                        new ArrayAdapter<String>(ObservationEditor.this,
                                android.R.layout.simple_list_item_1, items), null);

                final AlertDialog alertDialog = builder.create();

                ListView listView = alertDialog.getListView();
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        alertDialog.dismiss();

                        if (position == 0) {
                            // Get current location
                            getLocation();

                            mLocationProgressView.setVisibility(View.VISIBLE);
                            mLocationRefreshButton.setVisibility(View.GONE);
                            if (!mIsConfirmation) mLocationStopRefreshButton.setVisibility(View.VISIBLE);
                        } else {
                            // Edit location
                            Intent intent = new Intent(ObservationEditor.this, LocationChooserActivity.class);
                            intent.putExtra(LocationChooserActivity.LONGITUDE, mObservation.longitude);
                            intent.putExtra(LocationChooserActivity.LATITUDE, mObservation.latitude);
                            intent.putExtra(LocationChooserActivity.ACCURACY, (mObservation.positional_accuracy != null ? mObservation.positional_accuracy.doubleValue() : 0));

                            startActivityForResult(intent, LOCATION_CHOOSER_REQUEST_CODE);
                        }
                    }
                });

                alertDialog.show();
            }
        });

        if (!mIsConfirmation) {
            mLocationStopRefreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopGetLocation();
                }
            });

            mGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView parent, View view, int position, long id) {
                    Uri uri = ((GalleryCursorAdapter) parent.getAdapter()).getItemUri(position);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "image/*");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Failed to view photo: " + e);
                    }
                }
            });
        }

        if (getCurrentFocus() != null) {
            // Hide keyboard
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        
        
        if (mProjectIds == null) {
        	if ((intent != null) && (intent.hasExtra(OBSERVATION_PROJECT))) {
        		Integer projectId = intent.getIntExtra(OBSERVATION_PROJECT, 0);
        		mProjectIds = new ArrayList<Integer>();
        		mProjectIds.add(projectId);

        	} else {
        		// Get IDs of project-observations
        		int obsId = (mObservation.id == null ? mObservation._id : mObservation.id);
        		Cursor c = getContentResolver().query(ProjectObservation.CONTENT_URI, ProjectObservation.PROJECTION,
        				"(observation_id = " + obsId + ") AND ((is_deleted = 0) OR (is_deleted is NULL))",
        				null, ProjectObservation.DEFAULT_SORT_ORDER);
        		c.moveToFirst();
        		mProjectIds = new ArrayList<Integer>();
        		while (c.isAfterLast() == false) {
        			ProjectObservation projectObservation = new ProjectObservation(c);
        			mProjectIds.add(projectObservation.project_id);
        			c.moveToNext();
        		}
        		c.close();
        	}
        }

        refreshProjectFields();

        mProjectReceiver = new ProjectReceiver();
        IntentFilter filter = new IntentFilter(INaturalistService.ACTION_JOINED_PROJECTS_RESULT);
        registerReceiver(mProjectReceiver, filter);  
        
        if (mProjects == null) {
            Intent serviceIntent = new Intent(INaturalistService.ACTION_GET_JOINED_PROJECTS, null, this, INaturalistService.class);
            startService(serviceIntent);  
        } else {
            refreshProjectList();
        }


        if ((intent != null) && (!mPictureTaken)) {
            if (intent.getBooleanExtra(TAKE_PHOTO, false)) {
                // Immediately take a photo
                mFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
                mFileUri = getPath(ObservationEditor.this, mFileUri);

                final Intent galleryIntent = new Intent();

                galleryIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                galleryIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileUri);
                this.startActivityForResult(galleryIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

                // In case a new/existing photo was taken - make sure we won't retake it in case the activity pauses/resumes.
                mPictureTaken = true;

            } else if (intent.getBooleanExtra(CHOOSE_PHOTO, false)) {
                // Immediately choose an existing photo
                mFileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
                mFileUri = getPath(ObservationEditor.this, mFileUri);

                final Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                this.startActivityForResult(galleryIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

                // In case a new/existing photo was taken - make sure we won't retake it in case the activity pauses/resumes.
                mPictureTaken = true;
            }
        }

        if (mIsConfirmation) updateObservationVisibilityDescription();
    }
    
    @Override
    public void onBackPressed() {
        onBack();
    }
    
    private boolean onBack() {
        Observation observationCopy = new Observation(mCursor);
        uiToObservation();
        if (!mObservation.isDirty()) {
            // User hasn't changed anything - no need to display confirmation dialog
            mCanceled = true;
            finish();
            return true;
        }

        // Restore the old observation (since uiToObservation has overwritten it)
        mObservation = observationCopy;

        // Display a confirmation dialog
        confirm(ObservationEditor.this, R.string.edit_observation, R.string.discard_changes,
                R.string.yes, R.string.no,
                new Runnable() {
                    public void run() {
                        // Get back to the observations list (consider this as canceled)
                        mCanceled = true;
                        finish();
                    }
                },
                null);

        return false;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            return onBack();
        case R.id.save_observation:
            uiToProjectFieldValues();
            if (save()) {
                finish();
            }
            return true;
        }
        return true;
    } 

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        if (mFileUri != null) { outState.putString("mFileUri", mFileUri.toString()); }
        if (mUri != null) { outState.putString("mUri", mUri.toString()); }
        uiToObservation();
        outState.putSerializable("mObservation", mObservation);
        outState.putSerializable("mProjects", mProjects);
        outState.putIntegerArrayList("mProjectIds", mProjectIds);
        uiToProjectFieldValues();
        outState.putSerializable("mProjectFieldValues", mProjectFieldValues);
        outState.putBoolean("mProjectFieldsUpdated", mProjectFieldsUpdated);
        outState.putBoolean("mIsConfirmation", mIsConfirmation);
        outState.putBoolean("mPictureTaken", mPictureTaken);
        outState.putString("mPreviousTaxonSearch", mPreviousTaxonSearch);
        outState.putString("mTaxonPicUrl", mTaxonPicUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (mProjectReceiver != null) {
            try {
                unregisterReceiver(mProjectReceiver);  
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
        
        stopGetLocation();
        uiToProjectFieldValues();
        if (isFinishing()) {
        	
        	if (!mDeleted) {
        		if (isDeleteable()) {
        			delete(true);
        		} else if (!mCanceled) {
        			save();
        		}
        	}
        }
    }
    
    private boolean validateProjectFields() {
        if (!mIsConfirmation) {
            for (ProjectFieldViewer fieldViewer : mProjectFieldViewers) {
                ProjectField field = fieldViewer.getField();

                if (!fieldViewer.isValid()) {
                    Toast.makeText(this, String.format(getString(R.string.invalid_project_field), field.name), Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private void uiToProjectFieldValues() {
        int obsId = (mObservation.id == null ? mObservation._id : mObservation.id);

        if (!mIsConfirmation) {
            for (ProjectFieldViewer fieldViewer : mProjectFieldViewers) {
                ProjectField field = fieldViewer.getField();
                ProjectFieldValue fieldValue = mProjectFieldValues.get(field.field_id);

                if (fieldValue == null) {
                    // Create new field value
                    fieldValue = new ProjectFieldValue();
                    fieldValue.field_id = field.field_id;
                    fieldValue.observation_id = obsId;

                    mProjectFieldsUpdated = true;
                }

                // Overwrite value
                String newValue = fieldViewer.getValue();
                if ((newValue != null) && (!newValue.equals(fieldValue.value))) {
                    mProjectFieldsUpdated = true;

                    fieldValue.value = newValue;
                    mProjectFieldValues.put(field.field_id, fieldValue);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initUi();
        if (app == null) {
            app = (INaturalistApp) getApplicationContext();
        }
    }

    private void initUi() {
        if (mCursor == null) {
            mCursor = managedQuery(mUri, Observation.PROJECTION, null, null, null);
        } else {
            mCursor.requery();
        }

        if (mObservation == null) {
            mObservation = new Observation(mCursor);
        }
        
        if ((mSpeciesGuess != null) && (mObservation.species_guess == null)) {
            mObservation.species_guess = mSpeciesGuess;
        }


        if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            if (mObservation.observed_on == null) {
                mObservation.observed_on = mObservation.observed_on_was = new Timestamp(System.currentTimeMillis());
                mObservation.time_observed_at = mObservation.time_observed_at_was = mObservation.observed_on;
                mObservation.observed_on_string = mObservation.observed_on_string_was = app.formatDatetime(mObservation.time_observed_at);
                if (mObservation.latitude == null && mCurrentLocation == null) {
                    getLocation();
                }
            }
        }
        
        if (mObservation.id == null) {
            // Unsynced observation - don't allow adding new comments/ids
            if (!mIsConfirmation) {
                mObservationCommentsIds.setVisibility(View.GONE);
                mViewOnInat.setVisibility(View.GONE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mDeleteButton.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                mDeleteButton.setLayoutParams(params);
            }
        }
        
        updateUi();
    }

    private void updateUi() {
        observationToUi();

        updateImages();
    }

    private void uiToObservation() {
        if (((mObservation.species_guess == null) && (mSpeciesGuessTextView.getText().length() > 0) && (!mIsTaxonUnknown)) || (mObservation.species_guess != null)) mObservation.species_guess = mSpeciesGuessTextView.getText().toString();
        if (((mObservation.description == null) && (mDescriptionTextView.getText().length() > 0)) || (mObservation.description != null)) mObservation.description = mDescriptionTextView.getText().toString();
        if (mObservedOnStringTextView.getText() == null || mObservedOnStringTextView.getText().length() == 0) {
            mObservation.observed_on_string = null; 
        } else {
            mObservation.observed_on_string = mObservedOnStringTextView.getText().toString();
            mObservation.observed_on = mDateSetByUser;
            mObservation.time_observed_at = mTimeSetByUser;
        }

        if (mLatitudeView.getText() == null || mLatitudeView.getText().length() == 0) {
            mObservation.latitude = null;
        } else {
            mObservation.latitude = Double.parseDouble(mLatitudeView.getText().toString());
        }
        if (mLongitudeView.getText() == null || mLongitudeView.getText().length() == 0) {
            mObservation.longitude = null;
        } else {
            mObservation.longitude = Double.parseDouble(mLongitudeView.getText().toString());
        }
        if (mAccuracyView.getText() == null || mAccuracyView.getText().length() == 0) {
            mObservation.positional_accuracy = null;
        } else {
            mObservation.positional_accuracy = ((Float) Float.parseFloat(mAccuracyView.getText().toString())).intValue();
        }

        mObservation.id_please = mIdPlease.isChecked();

        List<String> values = Arrays.asList(getResources().getStringArray(R.array.geoprivacy_values));
        String selectedValue = values.get(mGeoprivacy.getSelectedItemPosition());
        if ((mObservation.geoprivacy != null) || (mGeoprivacy.getSelectedItemPosition() != 0)) {
            mObservation.geoprivacy = selectedValue;
        }

        if (mIsConfirmation) {
            boolean isCaptive = ((CheckBox)findViewById(R.id.is_captive_checkbox)).isChecked();
            if ((mObservation.captive != null) || ((mObservation.captive == null) && (isCaptive))) {
                mObservation.captive = isCaptive;
            }
        }
    }

    private void updateObservationVisibilityDescription() {
        List<String> names = Arrays.asList(getResources().getStringArray(R.array.geoprivacy_items));
        String selectedName = names.get(mGeoprivacy.getSelectedItemPosition());
        ((TextView)findViewById(R.id.location_visibility_description)).setText(getString(R.string.location_is) + " " + selectedName);
    }

    private void observationToUi() {
        List<String> values = Arrays.asList(getResources().getStringArray(R.array.geoprivacy_values));

        if (mObservation.geoprivacy != null) {
            mGeoprivacy.setSelection(values.indexOf(mObservation.geoprivacy));
        } else {
            mGeoprivacy.setSelection(0);
        }
        if (mIsConfirmation) updateObservationVisibilityDescription();

        mIdPlease.setChecked(mObservation.id_please);

        mSpeciesGuessTextView.setText(mIsTaxonUnknown ? "Unknown" : mObservation.species_guess);
        mDescriptionTextView.setText(mObservation.description);
        if (mObservation.observed_on == null) {
            mObservedOnButton.setText(getString(R.string.set_date));
        } else {
            mObservedOnButton.setText(app.shortFormatDate(mObservation.observed_on));
        }
        if (mObservation.observed_on_string != null) {
            mObservedOnStringTextView.setText(mObservation.observed_on_string);
            mDateSetByUser = mObservation.observed_on;
        }
        if (mObservation.time_observed_at == null) {
            mTimeObservedAtButton.setText(getString(R.string.set_time));
        } else {
            mTimeObservedAtButton.setText(app.shortFormatTime(mObservation.time_observed_at));
            mTimeSetByUser = mObservation.time_observed_at;
        }

        if (mObservation.latitude != null) {
            mLatitudeView.setText(mObservation.latitude.toString());
            if (mIsConfirmation) {
                findViewById(R.id.coordinates).setVisibility(View.VISIBLE);
            }
        } else {
            mLatitudeView.setText("");
        }
        if (mObservation.longitude != null) {
            mLongitudeView.setText(mObservation.longitude.toString());
        } else {
            mLongitudeView.setText("");
        }
        if (mObservation.positional_accuracy == null) {
            mAccuracyView.setText("");

            if (mIsConfirmation) {
                findViewById(R.id.accuracy_prefix).setVisibility(View.GONE);
                findViewById(R.id.accuracy).setVisibility(View.GONE);
            }
        } else {
            mAccuracyView.setText(mObservation.positional_accuracy.toString());
            if (mIsConfirmation) {
                findViewById(R.id.accuracy_prefix).setVisibility(View.VISIBLE);
                findViewById(R.id.accuracy).setVisibility(View.VISIBLE);
            }
        }

        if (!mIsConfirmation) {
            refreshCommentsIdSize(mObservation.updatesCount());
            if (mObservation.unviewedUpdates()) {
                mObservationCommentsIds.setBackgroundResource(R.drawable.comments_ids_background_highlighted);
            }
        } else {
            ((CheckBox)findViewById(R.id.is_captive_checkbox)).setChecked(mObservation.captive);

            if (mObservation.place_guess != null) {
                ((TextView) findViewById(R.id.location_guess)).setText(mObservation.place_guess);
            } else {
                ((TextView) findViewById(R.id.location_guess)).setText(getString(R.string.set_location));
            }
        }
    }

    /**
     * CRUD WRAPPERS
     */

    private final Boolean isDeleteable() {
        if (mCursor == null) { return true; }
        Cursor c = getContentResolver().query(mUri, new String[] {Observation._ID}, null, null, null);
        if (c.getCount() == 0) { return true; }
        if (mImageCursor != null && mImageCursor.getCount() > 0) { return false; }

        if (!mIsConfirmation) {
            if (mSpeciesGuessTextView.length() == 0
                    && mDescriptionTextView.length() == 0
                    && mObservedOnButton.length() == 0
                    && mTimeObservedAtButton.length() == 0
                    && mLatitudeView.length() == 0
                    && mLongitudeView.length() == 0) {
                return true;
            }
        }
        if (Intent.ACTION_INSERT.equals(getIntent().getAction()) && mCanceled) return true;
        return false;
    }

    private final boolean save() {
        if (mCursor == null) { return true; }
        
        if (!validateProjectFields()) {
            return false;
        }

        uiToObservation();
        
        boolean updatedProjects = saveProjects();
        saveProjectFields();
        
        if ((Intent.ACTION_INSERT.equals(getIntent().getAction())) || (mObservation.isDirty()) || (mProjectFieldsUpdated) || (updatedProjects)) {
            try {
                ContentValues cv = mObservation.getContentValues();
                if (mObservation.latitude_changed()) {
                    cv.put(Observation.POSITIONING_METHOD, "gps");
                    cv.put(Observation.POSITIONING_DEVICE, "gps");
                }
                getContentResolver().update(mUri, cv, null, null);
            } catch (NullPointerException e) {
                Log.e(TAG, "failed to save observation:" + e);
            }
        }
        
        app.checkSyncNeeded();
        
        return true;
    }

    private final void delete(boolean deleteLocal) {
        if (mCursor == null) { return; }
        
        if (deleteLocal) {
            try {
                getContentResolver().delete(mUri, null, null);
            } catch (NullPointerException e) {
                Log.e(TAG, "Failed to delete observation: " + e);
            }
        } else {
            // Only mark as deleted (so we'll later on sync the deletion)
            ContentValues cv = mObservation.getContentValues();
            cv.put(Observation.IS_DELETED, 1);
            getContentResolver().update(mUri, cv, null, null);
        }
        
        mDeleted = true;
        
        app.checkSyncNeeded();
    }

    /**
     * MENUS
     */

    /*
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.gallery_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.delete:
            Log.d(TAG, "selected delete from menu");
            deletePhoto(info.position);
        default:
            return super.onContextItemSelected(item);
        }
    }
    */

    /** Create a file Uri for saving an image or video */
    private Uri getOutputMediaFileUri(int type){
        ContentValues values = new ContentValues();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String name = "observation_" + mObservation._created_at.getTime() + "_" + timeStamp;
        values.put(android.provider.MediaStore.Images.Media.TITLE, name);
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    /**
     * Date/Time Pickers
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int month, int day) {
            Timestamp refDate;
            Timestamp date;
            try {
                refDate = new Timestamp(INaturalistApp.DATETIME_FORMAT.parse(mObservedOnStringTextView.getText().toString()).getTime());
                date = new Timestamp(year - 1900, month, day, refDate.getHours(), refDate.getMinutes(), refDate.getSeconds(), refDate.getNanos());
                if (date.getTime() > System.currentTimeMillis()) {
                    date = new Timestamp(System.currentTimeMillis());
                }
                mObservedOnStringTextView.setText(INaturalistApp.DATETIME_FORMAT.format(date));
                mObservedOnButton.setText(app.shortFormatDate(date));
            } catch (ParseException dateTimeException) {
                date = new Timestamp(year - 1900, month, day, 0, 0, 0, 0);
                if (date.getTime() > System.currentTimeMillis()) {
                    date = new Timestamp(System.currentTimeMillis());
                }
                mObservedOnStringTextView.setText(app.formatDate(date));
                mObservedOnButton.setText(app.shortFormatDate(date));
            }

            mDateSetByUser = date;
        }
    };

    private Timestamp mDateSetByUser;
    private Timestamp mTimeSetByUser;

    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hour, int minute) {
            Timestamp refDate;
            Date date;
            try {
                date = INaturalistApp.DATETIME_FORMAT.parse(mObservedOnStringTextView.getText().toString());
                refDate = new Timestamp(date.getTime());
            } catch (ParseException dateTimeException) {
                try {
                    date = INaturalistApp.DATE_FORMAT.parse(mObservedOnStringTextView.getText().toString());
                    refDate = new Timestamp(date.getTime());
                } catch (ParseException dateException) {
                    if (mObservation.time_observed_at != null) {
                        refDate = mObservation.time_observed_at; 
                    } else if (mObservation.observed_on != null) { 
                        refDate = mObservation.observed_on;
                    } else {
                        refDate = new Timestamp(System.currentTimeMillis());
                    }
                }
            }
            Timestamp datetime = new Timestamp(refDate.getYear(), refDate.getMonth(), refDate.getDate(), hour, minute, 0, 0);
            if (datetime.getTime() > System.currentTimeMillis()) {
                datetime = new Timestamp(System.currentTimeMillis());
            }
            mObservedOnStringTextView.setText(app.formatDatetime(datetime));
            mTimeObservedAtButton.setText(app.shortFormatTime(datetime));
            mTimeSetByUser = datetime;
        }
    };
    private ArrayList<Integer> mProjectIds;
    private Hashtable<Integer, ProjectField> mProjectFields;
    private HashMap<Integer, ProjectFieldValue> mProjectFieldValues = null;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DATE_DIALOG_ID:
            Timestamp refDate;
            if (mObservation.observed_on != null) {
                refDate = mObservation.observed_on;
            } else {
                refDate = new Timestamp(Long.valueOf(System.currentTimeMillis()));
            }
            try {
                return new DatePickerDialog(this, mDateSetListener, 
                        refDate.getYear() + 1900,
                        refDate.getMonth(),
                        refDate.getDate());
            } catch (IllegalArgumentException e) {
                refDate = new Timestamp(Long.valueOf(System.currentTimeMillis()));
                return new DatePickerDialog(this, mDateSetListener, 
                        refDate.getYear() + 1900,
                        refDate.getMonth(),
                        refDate.getDate());   
            }
        case TIME_DIALOG_ID:
            if (mObservation.time_observed_at != null) {
                refDate = mObservation.time_observed_at;
            } else {
                refDate = new Timestamp(Long.valueOf(System.currentTimeMillis()));
            }
            return new TimePickerDialog(this, mTimeSetListener, 
                    refDate.getHours(),
                    refDate.getMinutes(),
                    false);
        }
        return null;
    }

    /**
     * Location
     */

    // Kicks of location service
    private void getLocation() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }

        if (mLocationListener == null) {
            // Define a listener that responds to location updates
            mLocationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.
                    handleNewLocation(location);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}
            };
        }

        // Register the listener with the Location Manager to receive location updates
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);   
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        }

        mLocationRequestedAt = System.currentTimeMillis();
    }

    private void handleNewLocation(Location location) {
        if (isBetterLocation(location, mCurrentLocation)) {
            setCurrentLocation(location);
        }

        if (locationIsGood(mCurrentLocation)) {
            // Log.d(TAG, "location was good, removing updates.  mCurrentLocation: " + mCurrentLocation);
            stopGetLocation();
        }

        if (locationRequestIsOld() && locationIsGoodEnough(mCurrentLocation)) {
            // Log.d(TAG, "location request was old and location was good enough, removing updates.  mCurrentLocation: " + mCurrentLocation);
            stopGetLocation();
        }
    }

    private void stopGetLocation() {
        mLocationProgressView.setVisibility(View.GONE);
        mLocationRefreshButton.setVisibility(View.VISIBLE);

        if (!mIsConfirmation) {
            mLocationStopRefreshButton.setVisibility(View.GONE);
        }

        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }


    private void guessLocation() {
        if ((mObservation.latitude == null) || (mObservation.longitude == null)) {
            return;
        }

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(mObservation.latitude, mObservation.longitude, 1);
            if((null != addresses) && (addresses.size() > 0)) {
                Address address = addresses.get(0);
                StringBuilder location = new StringBuilder();
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    location.append(address.getAddressLine(i));
                    location.append(" ");
                }

                if ((location != null) && (location.length() > 0)) {
                    ((TextView) findViewById(R.id.location_guess)).setText(location);
                    mObservation.place_guess = location.toString().trim();
                } else {
                    ((TextView) findViewById(R.id.location_guess)).setText(R.string.set_location);
                    mObservation.place_guess = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setCurrentLocation(Location location) {
        mCurrentLocation = location;

        mObservation.latitude = location.getLatitude();
        mObservation.longitude = location.getLongitude();

        mLatitudeView.setText(Double.toString(location.getLatitude()));
        mLongitudeView.setText(Double.toString(location.getLongitude()));

        if (mIsConfirmation) {
            findViewById(R.id.coordinates).setVisibility(View.VISIBLE);
        }

        if (location.hasAccuracy()) {
            mAccuracyView.setText(Float.toString(location.getAccuracy()));
            mObservation.positional_accuracy = ((Float) location.getAccuracy()).intValue();
            if (mIsConfirmation) {
                findViewById(R.id.accuracy_prefix).setVisibility(View.VISIBLE);
                findViewById(R.id.accuracy).setVisibility(View.VISIBLE);
            }
        } else if (mIsConfirmation) {
            findViewById(R.id.accuracy_prefix).setVisibility(View.GONE);
            findViewById(R.id.accuracy).setVisibility(View.GONE);
        }

        if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            mObservation.latitude_was = mObservation.latitude;
            mObservation.longitude_was = mObservation.longitude;
            mObservation.positional_accuracy_was = mObservation.positional_accuracy;
        }

        if (mIsConfirmation) {
            guessLocation();
        }
    }

    private boolean locationRequestIsOld() {
        long delta = System.currentTimeMillis() - mLocationRequestedAt;
        return delta > ONE_MINUTE;
    }

    private boolean isBetterLocation(Location newLocation, Location currentLocation) {
        if (currentLocation == null) {
            return true;
        }
        if (newLocation.hasAccuracy() && !currentLocation.hasAccuracy()) {
            return true;
        }
        if (!newLocation.hasAccuracy() && currentLocation.hasAccuracy()) {
            return false;
        }
        return newLocation.getAccuracy() < currentLocation.getAccuracy();
    }

    private boolean locationIsGood(Location location) {
        if (!locationIsGoodEnough(location)) { return false; }
        if (location.getAccuracy() <= 10) {
            return true;
        }
        return false;
    }

    private boolean locationIsGoodEnough(Location location) {
        if (location == null || !location.hasAccuracy()) { return false; }
        if (location.getAccuracy() <= 500) { return true; }
        return false;
    }

    /**
     * MISC
     */
    
    private void saveProjectFields() {
        if (mIsConfirmation) {
            return;
        }

        for (ProjectFieldValue fieldValue : mProjectFieldValues.values()) {
            if (fieldValue.value == null) {
                continue;
            }
            
            if (fieldValue._id == null) {
                // New field value
                ContentValues cv = fieldValue.getContentValues();
                cv.put(ProjectFieldValue._SYNCED_AT, System.currentTimeMillis() - 100);
                Uri newRow = getContentResolver().insert(ProjectFieldValue.CONTENT_URI, cv);
                getContentResolver().update(newRow, fieldValue.getContentValues(), null, null);
            } else {
                // Update field value
                getContentResolver().update(fieldValue.getUri(), fieldValue.getContentValues(), null, null);
            }
        }
    }
     
   
    private boolean saveProjects() {
    	Boolean updatedProjects = false; // Indicates whether or not *any* projects were changed
        String joinedIds = StringUtils.join(mProjectIds, ",");
        
        int obsId = (mObservation.id == null ? mObservation._id : mObservation.id);

        // First, mark for deletion any projects that are no longer associated with this observation
        Cursor c = getContentResolver().query(ProjectObservation.CONTENT_URI, ProjectObservation.PROJECTION,
                "(observation_id = " + obsId + ") AND (project_id NOT IN (" + joinedIds + "))",
                null, ProjectObservation.DEFAULT_SORT_ORDER);

        c.moveToFirst();

        while (c.isAfterLast() == false) {
        	updatedProjects = true;
            ProjectObservation projectObservation = new ProjectObservation(c);
            projectObservation.is_deleted = true;
            getContentResolver().update(projectObservation.getUri(), projectObservation.getContentValues(), null, null);
            c.moveToNext();
        }
        c.close();


        // Next, unmark for deletion any project-observations which were re-added
        c = getContentResolver().query(ProjectObservation.CONTENT_URI, ProjectObservation.PROJECTION,
                "(observation_id = " + obsId + ") AND (project_id IN (" + joinedIds + "))",
                null, ProjectObservation.DEFAULT_SORT_ORDER);

        c.moveToFirst();

        ArrayList<Integer> existingIds = new ArrayList<Integer>();

        while (c.isAfterLast() == false) {
            ProjectObservation projectObservation = new ProjectObservation(c);
            if (projectObservation.is_deleted == true) {
            	updatedProjects = true;
            }
            projectObservation.is_deleted = false;
            existingIds.add(projectObservation.project_id);
            getContentResolver().update(projectObservation.getUri(), projectObservation.getContentValues(), null, null);
            c.moveToNext();
        }
        c.close();

        // Finally, add new project-observation records
        ArrayList<Integer> newIds = (ArrayList<Integer>) CollectionUtils.subtract(mProjectIds, existingIds);

        for (int i = 0; i < newIds.size(); i++) {
        	updatedProjects = true;
            int projectId = newIds.get(i);
            ProjectObservation projectObservation = new ProjectObservation();
            projectObservation.project_id = projectId;
            projectObservation.observation_id = obsId;
            projectObservation.is_new = true;
            projectObservation.is_deleted = false;

            getContentResolver().insert(ProjectObservation.CONTENT_URI, projectObservation.getContentValues());
        }
        
        return updatedProjects;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OBSERVATION_PHOTOS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Integer setFirstPhotoIndex = data.getIntExtra(ObservationPhotosViewer.SET_DEFAULT_PHOTO_INDEX, -1);
                Integer deletePhotoIndex = data.getIntExtra(ObservationPhotosViewer.DELETE_PHOTO_INDEX, -1);
                if (setFirstPhotoIndex > -1) {
                    ((GalleryCursorAdapter)mGallery.getAdapter()).setAsFirstPhoto(setFirstPhotoIndex);
                } else if (deletePhotoIndex > -1) {
                    deletePhoto(deletePhotoIndex);
                }
            }
        } else if (requestCode == LOCATION_CHOOSER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
            	stopGetLocation();
            	
                double longitude = data.getDoubleExtra(LocationChooserActivity.LONGITUDE, 0);
                double latitude = data.getDoubleExtra(LocationChooserActivity.LATITUDE, 0);
                double accuracy = data.getDoubleExtra(LocationChooserActivity.ACCURACY, 0);
                mObservation.latitude = latitude;
                mObservation.longitude = longitude;
                mObservation.positional_accuracy = (int) Math.ceil(accuracy);

                mLatitudeView.setText(Double.toString(latitude));
                mLongitudeView.setText(Double.toString(longitude));
                mAccuracyView.setText(mObservation.positional_accuracy.toString());
                if (mIsConfirmation) {
                    findViewById(R.id.coordinates).setVisibility(View.VISIBLE);
                    findViewById(R.id.accuracy_prefix).setVisibility(View.VISIBLE);
                    findViewById(R.id.accuracy).setVisibility(View.VISIBLE);

                    guessLocation();
                }
            }
         } else if (requestCode == TAXON_SEARCH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String iconicTaxonName = data.getStringExtra(TaxonSearchActivity.ICONIC_TAXON_NAME);
                String taxonName = data.getStringExtra(TaxonSearchActivity.TAXON_NAME);
                String idName = data.getStringExtra(TaxonSearchActivity.ID_NAME);
                String idPicUrl = data.getStringExtra(TaxonSearchActivity.ID_PIC_URL);
                Integer taxonId = data.getIntExtra(TaxonSearchActivity.TAXON_ID, 0);

                if (taxonId == TaxonSearchActivity.UNKNOWN_TAXON_ID) {
                    mSpeciesGuess = null;
                    mObservation.species_guess = null;
                    mObservation.taxon_id = null;
                    mSpeciesGuessTextView.setText("Unknown");
                    mPreviousTaxonSearch = "Unknown";
                    mTaxonPicUrl = null;
                    mIsTaxonUnknown = true;

                    if (mIsConfirmation) {
                        ((EditText)mSpeciesGuessTextView).setSelection(mSpeciesGuessTextView.length());
                        mSpeciesGuessIcon.setImageResource(R.drawable.ic_species_guess_black_24dp);
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                            mSpeciesGuessIcon.setAlpha(0.6f);
                        }
                    }
                } else {
                    String speciesGuess = String.format("%s", idName);
                    mSpeciesGuess = speciesGuess;
                    mObservation.species_guess = speciesGuess;
                    mObservation.taxon_id = taxonId;
                    mWatchForSpeciesGuessChanges = false;
                    mSpeciesGuessTextView.setText(mSpeciesGuess);
                    mPreviousTaxonSearch = mSpeciesGuess;
                    mTaxonPicUrl = idPicUrl;
                    mIsTaxonUnknown = false;

                    if (mIsConfirmation) {
                        ((EditText)mSpeciesGuessTextView).setSelection(mSpeciesGuess.length());
                        ((EditText)mSpeciesGuessTextView).clearFocus();
                        UrlImageViewHelper.setUrlDrawable(mSpeciesGuessIcon, idPicUrl, R.drawable.ic_species_guess_black_24dp, new UrlImageViewCallback() {
                            @Override
                            public void onLoaded(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                                    mSpeciesGuessIcon.setAlpha(1.0f);
                                }
                            }

                            @Override
                            public Bitmap onPreSetBitmap(ImageView imageView, Bitmap loadedBitmap, String url, boolean loadedFromCache) {
                                return loadedBitmap;
                            }
                        });
                    }
                }

            }

            mWatchForSpeciesGuessChanges = true;

        } else if (requestCode == PROJECT_FIELD_TAXON_SEARCH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Notify the project fields that we returned from a taxon search
                for (ProjectFieldViewer viewer : mProjectFieldViewers) {
                    viewer.onTaxonSearchResult(data);
                }
            }
        } else if (requestCode == PROJECT_SELECTOR_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (!mIsConfirmation) {
                    ArrayList<Integer> projectIds = data.getIntegerArrayListExtra(ProjectSelectorActivity.PROJECT_IDS);
                    mProjectIds = projectIds;

                    refreshProjectFields();
                    refreshProjectList();
                }
            }
        } else if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final boolean isCamera;
                if ((data == null) || (data.getScheme() == null)) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    if(action == null) {
                        isCamera = false;
                    } else {
                        isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                Uri selectedImageUri;
                if(isCamera) {
                    selectedImageUri = mFileUri;
                } else {
                    selectedImageUri = data == null ? null : data.getData();
                    if (selectedImageUri == null) {
                        selectedImageUri = mFileUri;
                    } else {
                        selectedImageUri = getPath(this, selectedImageUri);
                    }
                }

                Log.v(TAG, String.format("%s: %s", isCamera, selectedImageUri));

                if (isCamera) {
                    // Image captured and saved to mFileUri specified in the Intent
                    Toast.makeText(this, getString(R.string.image_saved), Toast.LENGTH_LONG).show();
                }
                
                updateImageOrientation(selectedImageUri);
                Uri createdUri = createObservationPhotoForPhoto(selectedImageUri);
                
                if (createdUri == null) {
                	mHelper.alert(getResources().getString(R.string.alert_unsupported_media_type));
                	mFileUri = null;
                	return;
                }

                updateImages();
                if (!isCamera) {
                    if (!mIsConfirmation) {
                        promptImportPhotoMetadata(selectedImageUri);
                    }
               }
                
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
                Toast.makeText(this,  String.format(getString(R.string.something_went_wrong), mFileUri.toString()), Toast.LENGTH_LONG).show();
                Log.e(TAG, "camera bailed, requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + (data == null ? "null" : data.getData()));
            }
            mFileUri = null; // don't let this hang around
            
        } else if (requestCode == COMMENTS_IDS_REQUEST_CODE) {
            
            // We know that the user now viewed all of the comments needed to be viewed (no new comments/ids)
            mObservation.comments_count += data.getIntExtra(CommentsIdsActivity.NEW_COMMENTS, 0);
            mObservation.identifications_count += data.getIntExtra(CommentsIdsActivity.NEW_IDS, 0);
            mObservation.last_comments_count = mObservation.comments_count;
            mObservation.last_identifications_count = mObservation.identifications_count;
            mObservation.taxon_id = data.getIntExtra(CommentsIdsActivity.TAXON_ID, 0);
            
            String speciesGuess = data.getStringExtra(CommentsIdsActivity.SPECIES_GUESS);
            if (speciesGuess != null) {
            	mSpeciesGuess = speciesGuess;
            	mObservation.species_guess = speciesGuess;
            	mSpeciesGuessTextView.setText(mSpeciesGuess);
            }
            String iconicTaxonName = data.getStringExtra(CommentsIdsActivity.ICONIC_TAXON_NAME);
            if (iconicTaxonName != null) mObservation.iconic_taxon_name = iconicTaxonName;

            // Only update the last_comments/id_count fields
            ContentValues cv = mObservation.getContentValues();
            cv.put(Observation._SYNCED_AT, System.currentTimeMillis()); // No need to sync
            getContentResolver().update(mUri, cv, null, null);

            mObservationCommentsIds.setBackgroundResource(R.drawable.comments_ids_background);

            Integer totalCount = mObservation.comments_count + mObservation.identifications_count;
            refreshCommentsIdSize(totalCount);
        }

        if (Intent.ACTION_INSERT.equals(getIntent().getAction())) {
            // Returned from activity AND it's a new observation
            if (mObservation.longitude == null) {
                // Got stopped in the middle of retrieving GPS coordinates - try again
                getLocation();
            }
        }
    }
    
    private void refreshCommentsIdSize(Integer value) {
        ViewTreeObserver observer = mObservationCommentsIds.getViewTreeObserver();
        // Make sure the height and width of the rectangle are the same (i.e. a square)
        observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
			@Override
            public void onGlobalLayout() {
                int dimension = mObservationCommentsIds.getHeight();
                ViewGroup.LayoutParams params = mObservationCommentsIds.getLayoutParams();
                
                if (dimension > mObservationCommentsIds.getWidth()) {
                    // Only resize if there's enough room
                    params.width = dimension;
                    mObservationCommentsIds.setLayoutParams(params);
                }
                
                ViewTreeObserver observer = mObservationCommentsIds.getViewTreeObserver();
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    observer.removeGlobalOnLayoutListener(this);
                } else {
                    observer.removeOnGlobalLayoutListener(this);
                }  

            }
        });
        
        mObservationCommentsIds.setText(value.toString());
    }

    private Uri createObservationPhotoForPhoto(Uri photoUri) {
        ObservationPhoto op = new ObservationPhoto();
        Long photoId;
        try {
        	photoId = ContentUris.parseId(photoUri);
        } catch (Exception exc) {
        	// Not a supported media type (e.g. Google Drive)
        	exc.printStackTrace();
        	return null;
        }

        ContentValues cv = op.getContentValues();
        cv.put(ObservationPhoto._OBSERVATION_ID, mObservation._id);
        cv.put(ObservationPhoto.OBSERVATION_ID, mObservation.id);
        if (photoId > -1) {
            cv.put(ObservationPhoto._PHOTO_ID, photoId.intValue());
        }
        if (mGallery.getCount() == 0) {
            cv.put(ObservationPhoto.POSITION, 0);
        } else {
            cv.put(ObservationPhoto.POSITION, mGallery.getCount());
        }
        return getContentResolver().insert(ObservationPhoto.CONTENT_URI, cv);
    }
    
    private void promptImportPhotoMetadata(Uri photoUri) {
        final Uri uri = photoUri;
        confirm(ObservationEditor.this, R.string.import_metadata, R.string.import_metadata_desc, 
                R.string.yes, R.string.no, 
                new Runnable() { public void run() {
                    importPhotoMetadata(uri);
                }}, 
                null);
    }
    
    private void importPhotoMetadata(Uri photoUri) {
        String imgFilePath = imageFilePathFromUri(photoUri);
        
        if (imgFilePath == null) return;
        
        try {
            ExifInterface exif = new ExifInterface(imgFilePath);
            float[] latLng = new float[2];
            uiToObservation();
            if (exif.getLatLong(latLng)) {
                stopGetLocation();
                mObservation.latitude = (double) latLng[0];
                mObservation.longitude = (double) latLng[1];
                mObservation.positional_accuracy = null;
            } else {
                // Nullify the GPS coordinates
                mObservation.latitude = null;
                mObservation.longitude = null;
                mObservation.positional_accuracy = null;
            }
            String datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if (datetime != null) {
                SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                try {
                    Date date = exifDateFormat.parse(datetime);
                    Timestamp timestamp = new Timestamp(date.getTime());
                    mObservation.observed_on = timestamp;
                    mObservation.time_observed_at = timestamp;
                    mObservation.observed_on_string = app.formatDatetime(timestamp);

                    mObservedOnStringTextView.setText(app.formatDatetime(timestamp));
                    mTimeObservedAtButton.setText(app.shortFormatTime(timestamp));
                    mDateSetByUser = timestamp;
                    mTimeSetByUser = timestamp;
                } catch (ParseException e) {
                    Log.d(TAG, "Failed to parse " + datetime + ": " + e);
                }
            }
            observationToUi();
        } catch (IOException e) {
            Log.e(TAG, "couldn't find " + imgFilePath);
        }
    }
    
    private void deletePhoto(int position) {
    	GalleryCursorAdapter adapter = (GalleryCursorAdapter) mGallery.getAdapter();
    	Long photoId = adapter.getItemId(position);
    	getContentResolver().delete(ObservationPhoto.CONTENT_URI, "_photo_id = " + photoId, null);
    	updateImages();
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
    	return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
    	return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
    	return "com.android.providers.media.documents".equals(uri.getAuthority());
    } 
    
    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
    		String[] selectionArgs) {

    	Cursor cursor = null;
    	final String column = "_data";
    	final String[] projection = {
    			column
    	};

    	try {
    		cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
    				null);
    		if (cursor != null && cursor.moveToFirst()) {
    			final int column_index = cursor.getColumnIndexOrThrow(column);
    			return cursor.getString(column_index);
    		}
    	} finally {
    		if (cursor != null)
    			cursor.close();
    	}
    	return null;
    } 
    
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
	private Uri getPath(final Context context, final Uri uri) {
    	
    	final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    	// DocumentProvider
    	if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
    		// ExternalStorageProvider
    		if (isExternalStorageDocument(uri)) {
    			final String docId = DocumentsContract.getDocumentId(uri);
    			final String[] split = docId.split(":");
    			final String type = split[0];

    			if ("primary".equalsIgnoreCase(type)) {
    				return Uri.parse(Environment.getExternalStorageDirectory() + "/" + split[1]);
    			}

    		}
    		// DownloadsProvider
    		else if (isDownloadsDocument(uri)) {

    			final String id = DocumentsContract.getDocumentId(uri);
    			final Uri contentUri = ContentUris.withAppendedId(
    					Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

    			return contentUri;
    		}
    		// MediaProvider
    		else if (isMediaDocument(uri)) {
    			final String docId = DocumentsContract.getDocumentId(uri);
    			final String[] split = docId.split(":");
    			final String type = split[0];

    			Uri contentUri = null;
    			if ("image".equals(type)) {
    				contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    			} else if ("video".equals(type)) {
    				contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    			} else if ("audio".equals(type)) {
    				contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    			}

    			final String selection = "_id=?";
    			final String[] selectionArgs = new String[] {
    					split[1]
    			};


    			return ContentUris.withAppendedId(contentUri, Long.valueOf(split[1]));
    		}
    	}

    	// MediaStore (and general)
    	if ("content".equalsIgnoreCase(uri.getScheme())) {
    		return uri;
    	}
    	// File
    	else if ("file".equalsIgnoreCase(uri.getScheme())) {
    		return uri;
    	}
    	
    	return null;
    }
    

    private void updateImageOrientation(Uri uri) {
        String imgFilePath = imageFilePathFromUri(uri);
        
        if (imgFilePath == null) return;
        
        ContentValues values = new ContentValues();
        int degrees = -1;
        try {
            ExifInterface exif = new ExifInterface(imgFilePath);
            degrees = exifOrientationToDegrees(
                    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 
                            ExifInterface.ORIENTATION_NORMAL));
            values.put(MediaStore.Images.ImageColumns.ORIENTATION, degrees);
            getContentResolver().update(uri, values, null, null);
            
            String lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String d = exif.getAttribute(ExifInterface.TAG_DATETIME);
            Log.d(TAG, "lat: " + lat + ", d: " + d);
        } catch (IOException e) {
            Log.e(TAG, "couldn't find " + imgFilePath);
        } catch (Exception e) {
        	Log.e(TAG, "Couldn't update image orientation for path: " + uri);
            SharedPreferences pref = getSharedPreferences("iNaturalistPreferences", MODE_PRIVATE);
            String username = pref.getString("username", null);
            Crashlytics.log(Log.ERROR, TAG, String.format("Couldn't update image orientation for uri: %s; path: %s; username: %s; degress: %d; exception: %s",
                    uri, imgFilePath, username, degrees, e));
            Crashlytics.logException(e);
        }
    }
    
    private String imageFilePathFromUri(Uri uri) {
        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.Images.Media.DATA
        };
        

        Cursor c;
        try {
        	c = getContentResolver().query(uri, projection, null, null, null);
        } catch (Exception exc) {
        	exc.printStackTrace();
        	return null;
        }
        
        if ((c == null) || (c.getCount() == 0)) {
        	return null;
        }

        c.moveToFirst();
        String path = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        c.close();
        
        return path;
    }

    protected void updateImages() {
    	if (mObservation.id != null) {
    		mImageCursor = getContentResolver().query(ObservationPhoto.CONTENT_URI, 
    				ObservationPhoto.PROJECTION, 
    				"_observation_id=? or observation_id=?", 
    				new String[]{mObservation._id.toString(), mObservation.id.toString()}, 
    				ObservationPhoto.DEFAULT_SORT_ORDER);
    	} else {
     		mImageCursor = getContentResolver().query(ObservationPhoto.CONTENT_URI, 
    				ObservationPhoto.PROJECTION, 
    				"_observation_id=?", 
    				new String[]{mObservation._id.toString()}, 
    				"id ASC, _id ASC");
    	}
        mImageCursor.moveToFirst();
        mGallery.setAdapter(new GalleryCursorAdapter(this, mImageCursor));
    }

    public class GalleryCursorAdapter extends BaseAdapter {
        private Context mContext;
        private Cursor mCursor;
        private HashMap<Integer, View> mViews;

        public GalleryCursorAdapter(Context c, Cursor cur) {
            mContext = c;
            mCursor = cur;
            mViews = new HashMap<Integer, View>();
        }

        public int getCount() {
            return mCursor.getCount();
        }

        public Object getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor;
        }

        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getInt(mCursor.getColumnIndexOrThrow(ObservationPhoto._PHOTO_ID));
        }

        public Uri getItemUri(int position) {
            mCursor.moveToPosition(position);
            int imageId = mCursor.getInt(mCursor.getColumnIndexOrThrow(ObservationPhoto._PHOTO_ID));
            String imageUrl = mCursor.getString(mCursor.getColumnIndexOrThrow(ObservationPhoto.PHOTO_URL));
            
            if (imageUrl != null) {
                // Online photo
                return Uri.parse(imageUrl);
            }
            
            // Offline (local storage) photo
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId);
        }
        
        private Cursor findPhotoInStorage(Integer photoId) {
            Cursor imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.TITLE, MediaStore.Images.ImageColumns.ORIENTATION},
                    MediaStore.MediaColumns._ID + " = " + photoId, null, null);

            imageCursor.moveToFirst();
            return imageCursor;
        }

        public void setAsFirstPhoto(int position) {
            Long photoId = getItemId(position);

            // Set current photo to be positioned first
            mCursor.moveToPosition(position);
            ObservationPhoto op = new ObservationPhoto(mCursor);
            op.position = 0;
            getContentResolver().update(ObservationPhoto.CONTENT_URI, op.getContentValues(), "_photo_id = " + photoId, null);

            // Update the rest of the photos to be positioned afterwards
            int currentPosition = 1;
            int count = mCursor.getCount();
            mCursor.moveToPosition(0);

            do {
                if (mCursor.getPosition() != position) {
                    ObservationPhoto currentOp = new ObservationPhoto(mCursor);
                    currentOp.position = currentPosition;
                    getContentResolver().update(ObservationPhoto.CONTENT_URI, currentOp.getContentValues(), "_photo_id = " + currentOp._photo_id, null);
                    currentPosition++;
                }
            } while (mCursor.moveToNext());

            updateImages();
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            if (mViews.containsKey(position)) {
                return mViews.get(position);
            }

            mCursor.moveToPosition(position);

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup container = null;
            ImageView imageView;

            if (mIsConfirmation) {
                container = (ViewGroup) inflater.inflate(R.layout.observation_photo_gallery_item, null, false);
                imageView = (ImageView) container.findViewById(R.id.observation_photo);
            } else {
                imageView = new ImageView(mContext);
            }

            int imageId = mCursor.getInt(mCursor.getColumnIndexOrThrow(ObservationPhoto._ID));
            int photoId = mCursor.getInt(mCursor.getColumnIndexOrThrow(ObservationPhoto._PHOTO_ID));
            String imageUrl = mCursor.getString(mCursor.getColumnIndexOrThrow(ObservationPhoto.PHOTO_URL));

            if (imageUrl != null) {
                // Online photo
            	imageView.setLayoutParams(new TwoWayView.LayoutParams(400, 400));
                UrlImageViewHelper.setUrlDrawable(imageView, imageUrl);
            } else {
                // Offline photo
                Cursor pc = findPhotoInStorage(photoId);
                if (pc.getCount() == 0) {
                    // photo has been deleted, delete the corresponding db row
                    getContentResolver().delete(ObservationPhoto.CONTENT_URI, "_id = " + imageId, null);
                } else {
                    int orientation = pc.getInt(pc.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
                    Bitmap bitmapImage = MediaStore.Images.Thumbnails.getThumbnail(
                            getContentResolver(), 
                            photoId, 
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            (BitmapFactory.Options) null);
                    if (orientation != 0) {
                        Matrix matrix = new Matrix();
                        matrix.setRotate((float) orientation, bitmapImage.getWidth() / 2, bitmapImage.getHeight() / 2);
                        bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), matrix, true);
                    }
                    imageView.setImageBitmap(mIsConfirmation ? ImageUtils.centerCropBitmap(bitmapImage) : bitmapImage);
                }
            }

            if (mIsConfirmation) {
                RadioButton isFirst = (RadioButton) container.findViewById(R.id.observation_is_first);
                Integer obsPhotoPosition = mCursor.getInt(mCursor.getColumnIndexOrThrow(ObservationPhoto.POSITION));

                if ((obsPhotoPosition != null) && (obsPhotoPosition == 0)) {
                    isFirst.setChecked(true);
                    isFirst.setText(R.string.first);
                } else {
                    isFirst.setChecked(false);
                    isFirst.setText("");
                }

                imageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ObservationEditor.this, ObservationPhotosViewer.class);
                        intent.putExtra(ObservationPhotosViewer.OBSERVATION_ID, mObservation._id);
                        intent.putExtra(ObservationPhotosViewer.IS_NEW_OBSERVATION, true);
                        intent.putExtra(ObservationPhotosViewer.CURRENT_PHOTO_INDEX, position);
                        startActivityForResult(intent, OBSERVATION_PHOTOS_REQUEST_CODE);
                    }
                });

                isFirst.setTag(new Integer(position));
                isFirst.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                        if (value) {
                            int position = (Integer)compoundButton.getTag();

                            setAsFirstPhoto(position);

                        }
                    }
                });

                mViews.put(position, container);
                return container;
            } else {
                mViews.put(position, imageView);
                return imageView;
            }
        }
    }

    private int exifOrientationToDegrees(int orientation) {
        switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
            return 90;
        case ExifInterface.ORIENTATION_ROTATE_180:
            return 180;
        case ExifInterface.ORIENTATION_ROTATE_270:
            return -90;
        default:
            return 0;
        }

    }
    
    /**
     * Display a confirm dialog. 
     * @param activity
     * @param title
     * @param message
     * @param positiveLabel
     * @param negativeLabel
     * @param onPositiveClick runnable to call (in UI thread) if positive button pressed. Can be null
     * @param onNegativeClick runnable to call (in UI thread) if negative button pressed. Can be null
     */
    public final void confirm(
            final Activity activity, 
            final int title, 
            final int message,
            final int positiveLabel, 
            final int negativeLabel,
            final Runnable onPositiveClick,
            final Runnable onNegativeClick) {
        mHelper.confirm(getString(title), getString(message),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (onPositiveClick != null) onPositiveClick.run();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (onNegativeClick != null) onNegativeClick.run();
                    }
                },
                positiveLabel, negativeLabel);
    }

    
    private void refreshProjectFields() {
        int obsId = (mObservation.id == null ? mObservation._id : mObservation.id);
        
        mProjectFields = new Hashtable<Integer, ProjectField>();

        // Get project fields
        for (int projectId : mProjectIds) {
            Cursor c = getContentResolver().query(ProjectField.CONTENT_URI, ProjectField.PROJECTION,
                    "(project_id = " + projectId + ")",
                    null, ProjectField.DEFAULT_SORT_ORDER);
           
            c.moveToFirst();

            while (c.isAfterLast() == false) {
                ProjectField projectField = new ProjectField(c);
                mProjectFields.put(projectField.field_id, projectField);
                c.moveToNext();
            }
            c.close();

        }
        
        if (mProjectFieldValues == null) {
            // Get project field values
            mProjectFieldValues = new HashMap<Integer, ProjectFieldValue>();
            
            Cursor c = getContentResolver().query(ProjectFieldValue.CONTENT_URI, ProjectFieldValue.PROJECTION,
                    "(observation_id = " + obsId + ")",
                    null, ProjectFieldValue.DEFAULT_SORT_ORDER);

            c.moveToFirst();
            while (c.isAfterLast() == false) {
                ProjectFieldValue fieldValue = new ProjectFieldValue(c);
                mProjectFieldValues.put(fieldValue.field_id, fieldValue);
                
                if (!mProjectFields.containsKey(fieldValue.field_id)) {
                    // It's a custom non-project field
                    Cursor c2 = getContentResolver().query(ProjectField.CONTENT_URI, ProjectField.PROJECTION,
                            "(field_id = " + fieldValue.field_id + ")", null, ProjectField.DEFAULT_SORT_ORDER);
                    c2.moveToFirst();
                    if (!c2.isAfterLast()) {
                        ProjectField field = new ProjectField(c2);
                        mProjectFields.put(fieldValue.field_id, field);
                    }
                    c2.close();
                }
                
                c.moveToNext();
            }
            c.close();
        }
        
        // Prepare the fields for display
        
        ArrayList<Map.Entry<Integer, ProjectField>> fields = new ArrayList(mProjectFields.entrySet());
        Collections.sort(fields, new Comparator<Map.Entry<Integer, ProjectField>>() {
            @Override
            public int compare(Entry<Integer, ProjectField> lhs, Entry<Integer, ProjectField> rhs) {
                ProjectField field1 = lhs.getValue();
                ProjectField field2 = rhs.getValue();
                
                Integer projectId1 = (field1.project_id != null ? field1.project_id : Integer.valueOf(-1));
                Integer projectId2 = (field2.project_id != null ? field2.project_id : Integer.valueOf(-1));
                
                if (projectId1 == projectId2) {
                    // Same project - sort by position
                    Integer position1 = (field1.position != null ? field1.position : Integer.valueOf(0));
                    Integer position2 = (field2.position != null ? field2.position : Integer.valueOf(0));
                    
                    return position1.compareTo(position2);
                } else {
                    // Group fields together in the same project
                    return projectId1.compareTo(projectId2);
                }
            }
        });


        if (!mIsConfirmation) {

            mProjectFieldViewers = new ArrayList<ProjectFieldViewer>();
            mProjectFieldsTable.removeAllViews();

            Iterator<Map.Entry<Integer, ProjectField>> iterator = fields.iterator();

            while (iterator.hasNext()) {
                ProjectField field = iterator.next().getValue();
                ProjectFieldValue fieldValue = mProjectFieldValues.get(field.field_id);

                ProjectFieldViewer fieldViewer = new ProjectFieldViewer(field, fieldValue);
                mProjectFieldViewers.add(fieldViewer);

                // Add field viewer to table
                mProjectFieldsTable.addView(fieldViewer.getView());
            }
        }
        
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }	

    
    private void openImageIntent(Activity activity, Uri captureImageOutputFile, int requestCode) {

        // Camera
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = activity.getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, captureImageOutputFile);
            cameraIntents.add(intent);
        }

        // File system
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_source));

        // Add the camera options
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        activity.startActivityForResult(chooserIntent, requestCode);
    }    
}
