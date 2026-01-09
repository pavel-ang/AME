package edu.urv.mobileembeded;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;
import edu.urv.mobileembeded.model.CurrentWeatherResponse;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<CurrentWeatherResponse> forecastList;

    public ForecastAdapter(List<CurrentWeatherResponse> forecastList) {
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_list_item, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        CurrentWeatherResponse weather = forecastList.get(position);
        String forecastText = "Temp: " + weather.getMain().getTemp() + "°C, " + weather.getWeather().get(0).getDescription();
        holder.textView.setText(forecastText);

        String iconCode = weather.getWeather().get(0).getIcon();
        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        Picasso.get().load(iconUrl).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewForecastIcon);
            textView = itemView.findViewById(R.id.textViewForecast);
        }
    }
}
