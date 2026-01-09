package edu.urv.mobileembeded;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import edu.urv.mobileembeded.bluetooth.BluetoothListener;
import edu.urv.mobileembeded.bluetooth.BluetoothManager;
import edu.urv.mobileembeded.model.CurrentWeatherResponse;
import edu.urv.mobileembeded.model.ForecastResponse;
import edu.urv.mobileembeded.network.RetrofitClient;
import edu.urv.mobileembeded.network.WeatherApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements BluetoothListener {

    private static final int REQUEST_PERMISSIONS = 1;
    private BluetoothManager bluetoothManager;
    private WeatherApiService weatherApiService;

    private EditText editTextCity;
    private Button buttonSearch;
    private TextView textViewBoardTemp;
    private TextView textViewCity;
    private TextView textViewWeatherCondition;
    private ImageView imageViewWeatherIcon;
    private RecyclerView recyclerViewForecast;
    private ForecastAdapter forecastAdapter;
    private List<CurrentWeatherResponse> forecastList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextCity = findViewById(R.id.editTextCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        textViewBoardTemp = findViewById(R.id.textViewBoardTemp);
        textViewCity = findViewById(R.id.textViewCity);
        textViewWeatherCondition = findViewById(R.id.textViewWeatherCondition);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        recyclerViewForecast = findViewById(R.id.recyclerViewForecast);

        recyclerViewForecast.setLayoutManager(new LinearLayoutManager(this));
        forecastAdapter = new ForecastAdapter(forecastList);
        recyclerViewForecast.setAdapter(forecastAdapter);

        bluetoothManager = new BluetoothManager(this);
        weatherApiService = RetrofitClient.getClient().create(WeatherApiService.class);

        requestPermissions();

        buttonSearch.setOnClickListener(v -> {
            String city = editTextCity.getText().toString();
            if (!city.isEmpty()) {
                fetchWeatherData(city);
            }
        });
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchWeatherData(String cityName) {
        String apiKey = "3ee56a6552b2fe7b045421eec995a25c";
        String units = "metric";

        weatherApiService.getCurrentWeather(cityName, apiKey, units).enqueue(new Callback<CurrentWeatherResponse>() {
            @Override
            public void onResponse(Call<CurrentWeatherResponse> call, Response<CurrentWeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CurrentWeatherResponse weather = response.body();
                    textViewCity.setText(cityName);
                    textViewWeatherCondition.setText(weather.getWeather().get(0).getDescription());

                    String iconCode = weather.getWeather().get(0).getIcon();
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
                    Picasso.get().load(iconUrl).into(imageViewWeatherIcon);
                }
            }

            @Override
            public void onFailure(Call<CurrentWeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show();
            }
        });

        weatherApiService.getForecast(cityName, apiKey, units).enqueue(new Callback<ForecastResponse>() {
            @Override
            public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    forecastList.clear();
                    forecastList.addAll(response.body().getList());
                    forecastAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ForecastResponse> call, Throwable t) {
                // Handle failure
            }
        });
    }

    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> {
            textViewBoardTemp.setText(data + "°C");
        });
    }

    @Override
    public void onDeviceConnected(String deviceName) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.closeConnection();
    }
}
