package com.newz.draghicistefan.newz;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
{
    //We will use 2 hashmaps to store the titles and urls by id, and separate arraylists to store the contents
    Map<Integer, String> articleUrls=new HashMap<>();
    Map<Integer, String> articleTitles=new HashMap<>();
    ArrayList<Integer> articleIds=new ArrayList<>();
    ArrayList<String> titles=new ArrayList<>();
    ArrayList<String> urls=new ArrayList<>();
    ArrayList<String> contents=new ArrayList<>();
    //We declare the list view and the respective adapter
    ArrayAdapter arrayAdapter;
    ListView listView;

    SQLiteDatabase articlesDB;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView= (ListView) findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        //We set a OnItemClickListener interface on each item so that we can read the article in a full page.
        // We will also store the information regarding the url, title and content in the intent.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Log.i("AppInfo", urls.get(position));
                Intent i = new Intent(MainActivity.this, ArticleActivity.class);
                i.putExtra("articleUrl", urls.get(position));
                i.putExtra("content", contents.get(position));
                i.putExtra("title", titles.get(position));
                startActivity(i);
            }
        });

        //We create a database on the fly using the openOrCreateDatabase method from the current context, and create the table with fields we need
        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");
        updateListView();
        //We instantiate a DownloadTask object to retrieve the contents of the website based on its address
        DownloadTask task = new DownloadTask();
        try
        {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //We retrieve the url via a separate thread, using a class that extends the AsyncTask, which will take a string as a request and return another string
    public class DownloadTask extends AsyncTask<String, Void, String>
    {
        //We do all the retrieveing of the urls in the doInBackground method
        @Override
        protected String doInBackground(String... params)
        {
            //we initialize a string to null. Later this will be the url
            String result="";
            //We declare the url and the HttpURLConnection that we will use to connect to the data source
            URL url=null;
            HttpURLConnection urlConnection=null;

            try
            {
                //We initialize the url and the connection
                url=new URL(params[0]);
                urlConnection= (HttpURLConnection) url.openConnection();
                //We will need a InputStream and InputStreamReader objects to store ncoming data from the connection
                InputStream in=urlConnection.getInputStream();
                InputStreamReader reader=new InputStreamReader(in);
                //We store data red from the input stream reader into a variable
                int data=reader.read();
                //We pass thru the stored information and, as long it's not null, we convert the data into character,
                // which we then append to our original result variable tu build the url
                while (data!=-1)
                {
                    char current= (char) data;
                    result+=current;
                    data=reader.read();
                }
                //We store the result in a JSONArray object and clear the table
                JSONArray jsonArray = new JSONArray(result);
                articlesDB.execSQL("DELETE FROM articles");

                //We pass thru the stored information and we want to retrieve the first 20 article from the website
                for (int i = 0; i < 20; i++)
                {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String articleInfo = "";

                    while (data != -1)
                    {
                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }
                    //We store the retrieved article in a JSONObject instance that will contain the url of the article, title and content
                    JSONObject jsonObject = new JSONObject(articleInfo);
                    String articleTitle = jsonObject.getString("title");
                    String articleURL = jsonObject.getString("url");
                    String articleContent = "";
                    //After retrieving, we store the data in our database
                    articleIds.add(Integer.valueOf(articleId));
                    articleTitles.put(Integer.valueOf(articleId), articleTitle);
                    articleUrls.put(Integer.valueOf(articleId), articleURL);
                    String sql = "INSERT INTO articles (articleId, url, title, content) VALUES (? , ? , ? , ?)";
                    SQLiteStatement statement = articlesDB.compileStatement(sql);
                    statement.bindString(1, articleId);
                    statement.bindString(2, articleURL);
                    statement.bindString(3, articleTitle);
                    statement.bindString(4, articleContent);
                    statement.execute();
                }
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
            } catch (IOException e)
            {
                e.printStackTrace();
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s)
        {
            super.onPostExecute(s);
            //We refresh ur list view
            updateListView();
        }
    }

    //We update our list view to show the latest articles
    public void updateListView()
    {
        try
        {
            Log.i("UI UPDATED", "DONE");

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

            int contentIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");

            c.moveToFirst();

            titles.clear();
            urls.clear();

            while (c != null)
            {
                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
                contents.add(c.getString(contentIndex));
                c.moveToNext();
            }

            arrayAdapter.notifyDataSetChanged();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
