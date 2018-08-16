package com.newz.draghicistefan.newz;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleActivity extends AppCompatActivity
{
    //We declare a webview in which we will see our article
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //We instantiate the webview
        webView= (WebView) findViewById(R.id.webView);
        //We allow the execution of javascript code (if there is any) to properly display the page
        webView.getSettings().setJavaScriptEnabled(true);
        //We set a new webview client so that we see the content in our webview.
        // By default, if we wouldn't have done this, the system would ask us
        // to chose an appropiate web browser to view the page
        webView.setWebViewClient(new WebViewClient());

        //We get the data from the intent so that we know which article to show
        Intent i=getIntent();
        String url=i.getStringExtra("articleUrl");
        String content=i.getStringExtra("content");
        String title=i.getStringExtra("title");
        setTitle(title);
        webView.loadUrl(url);
    }
}
