package equipe.projetoes.utilis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;

import equipe.projetoes.models.Livro;

/**
 * Created by Victor on 5/8/2016.
 */
public class HttpHandler {
    private Context ctx;
    private JSONArray livrosJson;
    private List<Livro> livros;
    private Bitmap lastDraw;
    private int lastPosImageSet = 0;
    private boolean isReady = false;
    private int coverIndex;
    private int coverQt;
    private int coverQtPass;
    private int lastCoverNum = 0;
    private int lastBookNum = 0;

    public HttpHandler(Context ctx) {
        this.ctx = ctx;

        livros = new ArrayList<Livro>();
        getBooks(20);

    }

    public static String GET(String url) {
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) ctx.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        private int index;

        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(ctx, "Received!", Toast.LENGTH_LONG).show();
            //System.out.println(result);
            //etResponse.setText(result);


            JSONObject json = null; // convert String to JSONObject
            try {
                json = new JSONObject(result);

                livrosJson = json.getJSONArray("items"); // get articles array
                //livros.length(); // --> 2
                //livros.getJSONObject(0); // get first article in the array
                //livros.getJSONObject(0).names(); // get first article keys [title,url,categories,tags]
                //livros.getJSONObject(0).getString("url"); // return an article url


                index = 0;
                extractBooks();
                if (lastCoverNum == 0)
                    getCovers(0, 10);

                //System.out.println(livros.get(0).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }/* catch (IOException e) {
                e.printStackTrace();
            }*/

        }

        private void extractBooks() {
            try {
                JSONObject volume;
                Livro livro;
                for (int i = index; i < livrosJson.length(); i++) {
                    //System.out.println("for index " + i);

                    volume = (JSONObject) livrosJson.getJSONObject(i).get("volumeInfo");

                    livro = new Livro(((JSONObject) volume.get("imageLinks")).get("thumbnail").toString(),
                            volume.get("title").toString(),
                            ((JSONArray) volume.get("authors")).get(0).toString(),
                            volume.get("publisher").toString(),
                            volume.getInt("pageCount"),
                            0,
                            false,
                            false,
                            ((JSONObject) ((JSONArray) volume.get("industryIdentifiers")).get(0)).get("identifier").toString());
                    if (!livros.contains(livro))
                        livros.add(livro);

                    //  String drawable = ((JSONObject) volume.get("imageLinks")).get("thumbnail").toString();
                    // System.out.println(drawable);


                    //  new DrawableFromUrl().execute(drawable);
                    index++;
                }
            } catch (JSONException e) {
                index++;
                extractBooks();
            }
        }
    }

    public void getCovers(int init, int qt) {
        System.out.println("getCovers(" + init + "," + qt + ")");
        // coverQtPass = 0;
        // coverQt = qt;
        // coverIndex = init;

        if (lastCoverNum < livros.size()) {
            for (int i = init; i < init + qt; i++) {
                if (lastCoverNum > i) continue;
                lastCoverNum++;
                try {
                    new DrawableFromUrl().execute(livros.get(i).getUrlImg());
                } catch (IndexOutOfBoundsException e) {
                    break;

                }

                //System.out.println("for");
            }
        }
    }

    public void getBooks(int qt) {
        System.out.println("getBooks(" + qt + ")");
        new HttpAsyncTask().execute("https://www.googleapis.com/books/v1/users/109518442467553217123/bookshelves/1001/volumes?startIndex=" + livros.size() + "&maxResults=" + qt);

    }


    private class DrawableFromUrl extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            try {
                lastDraw = drawable_from_url(urls[0]);
                //coverQtPass++;
            } catch (IOException e) {
                e.printStackTrace();
                //getCovers(coverIndex++,coverQt-coverQtPass);
                return "error";
            }
            return "ok";
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            livros.get(lastPosImageSet).setDrawable(lastDraw);
            lastPosImageSet++;
            isReady = true;
        }

    }

    public Bitmap drawable_from_url(String url) throws java.net.MalformedURLException, java.io.IOException {
        Bitmap x;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-agent", "Mozilla/4.0");

        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return x;
    }


    public List<Livro> getLivros() {
        return livros;
    }

    public void setLivros(List<Livro> livros) {
        this.livros = livros;
    }

    public boolean isReady() {
        return isReady;
    }
}
