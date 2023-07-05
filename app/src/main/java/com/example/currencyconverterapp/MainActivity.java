package com.example.currencyconverterapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText amountEditText;
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private TextView resultTextView;
    private Button convertButton;

    private List<String> currencies;
    private JSONObject exchangeRates;

    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/USD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        amountEditText = findViewById(R.id.amount_edit_text);
        fromSpinner = findViewById(R.id.from_spinner);
        toSpinner = findViewById(R.id.to_spinner);
        resultTextView = findViewById(R.id.result_text_view);
        convertButton = findViewById(R.id.convert_button);

        currencies = new ArrayList<>();
        exchangeRates = new JSONObject();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);

        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertCurrency();
            }
        });

        new FetchExchangeRatesTask().execute();
    }

    private void convertCurrency() {
        double amount = Double.parseDouble(amountEditText.getText().toString());
        String fromCurrency = fromSpinner.getSelectedItem().toString();
        String toCurrency = toSpinner.getSelectedItem().toString();

        try {
            double fromRate = exchangeRates.getDouble(fromCurrency);
            double toRate = exchangeRates.getDouble(toCurrency);

            double result = (amount / fromRate) * toRate;

            DecimalFormat decimalFormat = new DecimalFormat("#,###.##");
            resultTextView.setText(decimalFormat.format(result));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchExchangeRatesTask extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... voids) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            JSONObject jsonResult = null;

            try {
                URL url = new URL(API_URL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder buffer = new StringBuilder();

                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                jsonResult = new JSONObject(buffer.toString());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return jsonResult;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null) {
                try {
                    JSONObject rates = jsonObject.getJSONObject("rates");
                    Iterator<String> iterator = rates.keys();
                    while (iterator.hasNext()) {
                        String currency = iterator.next();
                        currencies.add(currency);
                        exchangeRates.put(currency, rates.getDouble(currency));
                    }

                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) fromSpinner.getAdapter();
                    adapter.notifyDataSetChanged();

                    adapter = (ArrayAdapter<String>) toSpinner.getAdapter();
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
