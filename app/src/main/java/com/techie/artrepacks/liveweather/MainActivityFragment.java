package com.techie.artrepacks.liveweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    protected double pressure;
    int errorCode;
    String city;
    int humidity;
    private ArrayAdapter<String> mForecastAdapter;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.mainactivityfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.action_refresh)
        {
            updateweather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void getErrorCode()
    {
        if (errorCode == 1)
        {
            Toast.makeText(getContext(), "Connection Problem! Check your network connection", Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(getContext(), "Connected!", Toast.LENGTH_LONG).show();
        }
    }
    //Method to update weather
    private void updateweather()
    {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        weatherTask.execute(location);
        //getErrorCode();
        //alert();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        updateweather();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long length)
            {
                String forecast = mForecastAdapter.getItem(position)+ "\nCity: " + city;
                Intent intent = new Intent(getActivity(),DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });
        return rootView;
    }
    public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private String getReadableDate(long time)
        {
            SimpleDateFormat format = new SimpleDateFormat("EE, MMM d", Locale.ENGLISH);
            return format.format(time);
        }

        private String formatHighLows(double high, double low)
        {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPrefs.getString(getString(R.string.pref_units_key),getString(R.string.pref_units_metric));

            if(unitType.equals(getString(R.string.pref_units_imperial)))
            {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            }
            else if(!unitType.equals(getString(R.string.pref_units_metric)))
            {
                Log.d(LOG_TAG,"Unit not found:" + unitType);
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            //String highLowStr = roundedHigh +"/"+roundedLow;
		// Code to return °C and °F
		// if(unitType.equals(getString(R.string.pref_units_metric)))
//
//
            return roundedHigh+"/"+roundedLow;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int noOfDays)throws JSONException
        {
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";
            final String OWM_CITY = "city";
            final String OWM_NAME = "name";
            final String OWM_PRESSURE = "pressure";
            final String OWM_HUMIDITY = "humidity";
            final String OWM_WEATHERID = "id";
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            JSONObject cityObject = forecastJson.getJSONObject(OWM_CITY);
            city = cityObject.getString(OWM_NAME);
            //

            Log.i(LOG_TAG,"City is:"+city);
            Log.i(LOG_TAG,"Data:"+forecastJsonStr);
            Time dayTime = new Time();
            dayTime.setToNow();
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(),dayTime.gmtoff);
            dayTime = new Time();
            String[] resultStrs = new String[noOfDays];
            for (int i = 0;i<weatherArray.length();i++)
            {
                String day;
                String description;
                String highAndLow;

                JSONObject dayForecast = weatherArray.getJSONObject(i);
                long dateTime;
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDate(dateTime);

                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                int id = weatherObject.getInt(OWM_WEATHERID);
                Log.i(LOG_TAG,"ID: "+id);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                Log.i(LOG_TAG,"High:"+high);
                double low = temperatureObject.getDouble(OWM_MIN);
                //JSONObject to get pressure
                JSONObject pressureObject = weatherArray.getJSONObject(i);
                pressure = pressureObject.getDouble(OWM_PRESSURE);
                //JSONObject to get humidity
                JSONObject humidityObject = weatherArray.getJSONObject(i);
                humidity = humidityObject.getInt(OWM_HUMIDITY);
                //JSONObject to get weather <code>Hello</code>
                Log.i(LOG_TAG,"HUM:"+humidity);
                Log.i(LOG_TAG,"Pressure:"+pressure);
                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + "-" + description + "-" + highAndLow + "\nHumidity: " + humidity + "\nPressure: " + pressure ;
            }
            for (String s : resultStrs)
            {
                Log.v(LOG_TAG,"Forecast Entry: " + s);
            }
            return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... params){
            if(params.length ==0)
            {
                return null;
            }
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            String format = "json";
            String unit = "metric";
            final String apiKey= "6995c22f85d1e50c35b08c56f9be8d2c";
            int noOfDays = 7;
            //TODO:Code openweathermap
            //Code to get the data from the openweathermap
            try{
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(APPID_PARAM, apiKey)
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, unit)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(noOfDays))
                        .build();
                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG,"Builted URL:"+builtUri.toString());
                urlConnection = (HttpURLConnection)url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();
                if(inputStream == null)
                {
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine())!= null)
                {
                    buffer.append(line + "\n");
                }
                if (buffer.length()==0){
                    return null;
                }
                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG,"Forecast JSON Data:"+forecastJsonStr);
            }
            catch(IOException e){
                Log.e(LOG_TAG,"Error",e);
                errorCode = 1;

                return null;
            }
            finally {
                if (urlConnection != null){
                    urlConnection.disconnect();
                }
                if (reader != null)
                {
                    try{
                        reader.close();
                    }
                    catch (final IOException e){
                        Log.e(LOG_TAG,"Error closing stream",e);
                    }
                }
            }
            try{
                return getWeatherDataFromJson(forecastJsonStr, noOfDays);
            }
            catch (JSONException e)
            {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result)
        {
            if (result != null)
            {
                mForecastAdapter.clear();
                for (String dayForecastStr : result)
                {
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }

    }
}
