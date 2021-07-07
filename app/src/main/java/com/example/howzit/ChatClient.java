package com.example.howzit;


import java.io.BufferedReader;
import java.io.PrintWriter;

public class ChatClient {
    private BufferedReader inputData;
    private PrintWriter outputData;
    private String name;
    private Listener listener;

    public ChatClient(String name,BufferedReader inputData,PrintWriter outputData){
        this.inputData=inputData;
        this.outputData=outputData;
        this.name=name;

        listener=new Listener(inputData);
    }

    public String getName(){return name;}
    public PrintWriter getDataOut(){return outputData;}
    public void startListening(){
        listener.listening=true;
        listener.start();
    }
}

