package com.guet.iotforagriculture;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Documented;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final int IMG_MSG = 1;
    private final int TEXT_MSG = 2;
    private List<Msg> mData;
    MsgAdapter msgAdapter;
    private boolean flag = false;
    private boolean isDone = false;
    private String path;
    private String fileName;
    private BufferedReader reader;
    private PrintWriter writer;
    private DataInputStream dataReader;
    private InputStreamReader streamReader;
    private BufferedOutputStream outputStream;
    private BufferedInputStream inputStream;
    private Button btn_text;
    private EditText text_send;
    private ImageView imageView;

    @SuppressLint("HandlerLeak")
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case TEXT_MSG:
                    mData.add((Msg) msg.obj);
                    msgAdapter.notifyDataSetChanged();
                    break;

                case IMG_MSG:
                    try {
                        Bitmap image = (Bitmap) msg.obj;
                        imageView.setImageBitmap(image);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initNetwork();
        initUI();
        initWork();
    }

    public void initUI() {
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mData = new ArrayList<Msg>();
        msgAdapter = new MsgAdapter(mData);
        recyclerView.setAdapter(msgAdapter);
    }

    public void initNetwork() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(你要连接的服务器的外网地址, 5000);
                    socket.isConnected();
                    streamReader = new InputStreamReader(socket.getInputStream());
                    reader = new BufferedReader(streamReader);
                    dataReader = new DataInputStream(socket.getInputStream());
                    outputStream = new BufferedOutputStream(socket.getOutputStream());
                    writer = new PrintWriter(socket.getOutputStream());
                    Thread recvThread = new Thread(new RecvWork());
                    recvThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void initWork() {
        btn_text = findViewById(R.id.btn_text);
        text_send = findViewById(R.id.text_send);
        imageView = findViewById(R.id.test);
        btn_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //String message = text_send.getText().toString();
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(intent, IMG_MSG);
                        }
                        //writer.write(message);
                        //writer.flush();
                    }
                }).start();
            }
        });
    }

    public class RecvWork implements Runnable {
        @Override
        public void run() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if(line.equals("get")){
                        flag = true;
                        while(!isDone){

                        }
                        isDone = false;
                        System.out.println("send get");
                        String fileSize_str = reader.readLine();
                        int fileSize = Integer.parseInt(fileSize_str);
                        int recv = 0;
                        String[] pathSplit = path.split("/");
                        String[] fileNameS = fileName.split("\\.");
                        String fileName = fileNameS[0] + "_r." + fileNameS[1];
                        pathSplit[pathSplit.length-1] = fileName;
                        String new_path = new String();
                        for(int i = 1;i<pathSplit.length;i++){
                            String str = pathSplit[i];
                            new_path += ('/' + str);
                        }
                        File file_r = new File(new_path);
                        FileOutputStream fileOutputStream = new FileOutputStream(file_r);
                        byte[] bytes = new byte[1024];
                        char[] cb = new char[1024];
                        int len_;
                        writer.write("get");
                        writer.flush();
                        while((len_ = dataReader.read(bytes)) != -1){
                            fileOutputStream.write(bytes,0,len_);
                            recv += len_;

                            if(fileSize == recv) {
                                System.out.println(recv);
                                break;
                            }
                        }
                        Bitmap image = BitmapFactory.decodeFile(new_path);
                        Message msg = Message.obtain();
                        msg.obj = image;
                        msg.what = IMG_MSG;
                        handler.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == IMG_MSG && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Message msg = Message.obtain();
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            String type = split[0];
            Uri contentUri = null;
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String select = "_id=?";
            String[] selectArgs = new String[]{split[1]};
            String[] projection = new String[]{"_data"};
            Cursor cursor = getContentResolver().query(contentUri, projection, select, selectArgs, null);
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex("_data");
                path = cursor.getString(index);
                cursor.close();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = new File(path);
                    String[] pathSplit = path.split("/");
                    fileName = pathSplit[pathSplit.length - 1];
                    writer.write(fileName);
                    writer.flush();
                    try {
                        inputStream = new BufferedInputStream(new FileInputStream(file));
                        long fileSize = file.length();
                        writer.write(String.valueOf(fileSize));
                        writer.flush();
                        while (!flag){
                            //do nothing
                        }
                        flag = false;
                        int len;
                        byte[] buffer = new byte[1024];
                        while (((len = inputStream.read(buffer)) != -1)) {
                            outputStream.write(buffer,0,len);
                        }
                        outputStream.flush();
                        isDone = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }
}
