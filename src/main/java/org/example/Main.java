package org.example;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        OpenAIClient openAIClient = new OpenAIClient();

        // Example usage
        String datasetId = openAIClient.createDataset("C:\\Users\\MSI\\Desktop\\ApiChatgptdoc.json");
        String jobId = openAIClient.createFineTuningJob(datasetId);
        String jobStatus = openAIClient.getFineTuningJob(jobId);

        if ("succeeded".equals(jobStatus)) {
            String engineId = "your_finetuned_model_id";
            String prompt = "What is the return policy?";
            String completion = openAIClient.createCompletion(engineId, prompt);
            System.out.println(completion);
        }
    }

    public interface OpenAIApi {
        @POST("datasets")
        Call<ResponseBody> createDataset(@Body RequestBody requestBody);

        @POST("fine-tuning-jobs")
        Call<ResponseBody> createFineTuningJob(@Body RequestBody requestBody);

        @GET("fine-tuning-jobs/{job_id}")
        Call<ResponseBody> getFineTuningJob(@Path("job_id") String jobId);

        @POST("engines/{engine_id}/completions")
        Call<ResponseBody> createCompletion(@Path("engine_id") String engineId, @Body RequestBody requestBody);
    }


    public static class OpenAIClient {
        private static final String API_KEY = "your-api-key";
        private static final String BASE_URL = "https://api.openai.com/v1/";
        private final OpenAIApi openAIApi;

        public OpenAIClient() {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + API_KEY)
                        .method(original.method(), original.body());
                Request request = requestBuilder.build();
                return chain.proceed(request);
            });

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();

            openAIApi = retrofit.create(OpenAIApi.class);
        }

        // Implement methods to interact with the OpenAI API, like createDataset, createFineTuningJob, getFineTuningJob, and createCompletion

        public String createDataset(String filePath) throws IOException {
            Gson gson = new Gson();
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("file", filePath);
            requestBody.addProperty("name", "RetailShopDataset");
            requestBody.addProperty("purpose", "fine-tuning");

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestBody));
            retrofit2.Response<ResponseBody> response = openAIApi.createDataset(body).execute();

            if (response.isSuccessful()) {
                JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
                return responseBody.get("id").getAsString();
            }

            return null;
        }

        public String createFineTuningJob(String datasetId) throws IOException {
            Gson gson = new Gson();
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "gpt 4");
            requestBody.addProperty("dataset", datasetId);
            requestBody.addProperty("base_model", "gpt-4");
            JsonObject hyperparameters = new JsonObject();
            hyperparameters.addProperty("learning_rate", 3e-5);
            hyperparameters.addProperty("batch_size", 4);
            hyperparameters.addProperty("num_train_epochs", 3);
            requestBody.add("hyperparameters", hyperparameters);

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestBody));
            retrofit2.Response<ResponseBody> response = openAIApi.createFineTuningJob(body).execute();

            if (response.isSuccessful()) {
                JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
                return responseBody.get("id").getAsString();
            }

            return null;
        }

        public String getFineTuningJob(String jobId) throws IOException {
            Gson gson = new Gson();
            retrofit2.Response<ResponseBody> response = openAIApi.getFineTuningJob(jobId).execute();
            if (response.isSuccessful()) {
                JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
                return responseBody.get("status").getAsString();
            }

            return null;
        }

        public String createCompletion(String engineId, String prompt) throws IOException {
            Gson gson = new Gson();
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("prompt", prompt);
            requestBody.addProperty("max_tokens", 50);
            requestBody.addProperty("n", 1);
            requestBody.addProperty("temperature", 0.5);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(requestBody));
            retrofit2.Response<ResponseBody> response = openAIApi.createCompletion(engineId, body).execute();

            if (response.isSuccessful()) {
                JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
                JsonObject choice = responseBody.getAsJsonArray("choices").get(0).getAsJsonObject();
                return choice.get("text").getAsString().trim();
            }

            return null;
        }

    }

}
