package com.example.shihwei.listviewdemo;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import com.example.shihwei.listviewdemo.data.Destination;
import com.example.shihwei.listviewdemo.data.Record;
import com.example.shihwei.listviewdemo.data.Source;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static final boolean DEBUG = false;
    private static final String DEBUG_TAG = "Utils";

    public static URL buildUrl(long start, int count) {
        URL url = null;
        String link = String.format("https://hook.io/syshen/infinite-list?startIndex=%d&num=%d"
                , start, count);
        try {
            url = new URL(link);
        } catch (MalformedURLException e) {
            Log.e(DEBUG_TAG, "Error url=" + link);
        }
        return url;
    }

    // retrieve data from given URL and compose each data as a list
    public static List retrieveData(URL url) throws IOException {
        InputStream is = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // start query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();
            return readJsonStream(is);

            // close InputStream
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static List readJsonStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readRecordArray(reader);
        } finally {
            reader.close();
        }
    }

    private static List readRecordArray(JsonReader reader) throws IOException {
        List record = new ArrayList();

        reader.beginArray();
        while (reader.hasNext()) {
            record.add(readRecord(reader));
        }
        reader.endArray();
        return record;
    }

    private static Record readRecord(JsonReader reader) throws IOException {
        long id = -1;
        String created = null;
        Source source = null;
        Destination dest = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("id")) {
                id = reader.nextLong();
            } else if (name.equals("created")) {
                created = reader.nextString();
            } else if (name.equals("source")) {
                source = readSource(reader);
            } else if (name.equals("destination")) {
                dest = readDestination(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Record(id, created, source, dest);
    }

    private static Source readSource(JsonReader reader) throws IOException {
        String sender = null;
        String note = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("sender")) {
                sender = reader.nextString();
            } else if (name.equals("note") && reader.peek() != JsonToken.NULL) {
                note = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Source(sender, note);
    }

    private static Destination readDestination(JsonReader reader) throws IOException {
        String recipient = null;
        long amount = -1;
        String currency = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("recipient")) {
                recipient = reader.nextString();
            } else if (name.equals("amount")) {
                amount = reader.nextLong();
            } else if (name.equals("currency")) {
                currency = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Destination(recipient, amount, currency);
    }

    private void dump(Record record) {
        if (DEBUG) {
            StringBuffer sb = new StringBuffer();
            sb.append("id = " + record.getId());
            sb.append("\ncreated = " + record.getCreated());
            sb.append("\nsender = " + record.getSource().getSender());
            sb.append("\nnote = " + record.getSource().getNote());
            sb.append("\nrecipient = " + record.getDestination().getRecipient());
            sb.append("\namount = " + record.getDestination().getAmount());
            sb.append("\ncurrency = " + record.getDestination().getCurrency());
            Log.i(DEBUG_TAG, sb.toString());
        }
    }
}
