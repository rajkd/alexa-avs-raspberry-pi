package com.amazon.alexa.avs.speech;

import java.util.Arrays;
import java.util.List;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;

public class Transcriber extends Thread {

    private Configuration configuration;
    private LiveSpeechRecognizer recognizer;
    private TranscriberListener transcriberListener;
    private boolean transcriberEnabled = false;
    private List<String> triggerWords;

    private static final String ACOUSTIC_MODEL = "resource:/res/en-us";
    private static final String DICTIONARY_PATH = "resource:/res/cmudict-en-us.dict";
    private static final String GRAMMAR_PATH = "resource:/res/dialog/";
    private static final String LANGUAGE_MODEL = "resource:/res/en-us.lm";
    private static final String GRAMMAR_NAME = "start";
    

    public Transcriber(final TranscriberListener listener) throws Exception {   
        this.transcriberListener = listener;

        configuration = new Configuration();
        configuration.setAcousticModelPath(ACOUSTIC_MODEL);
        configuration.setDictionaryPath(DICTIONARY_PATH);
        configuration.setLanguageModelPath(LANGUAGE_MODEL);
        configuration.setGrammarPath(GRAMMAR_PATH);
        configuration.setUseGrammar(true);
        configuration.setGrammarName(GRAMMAR_NAME);

        recognizer = new LiveSpeechRecognizer(configuration);

        this.triggerWords = Arrays.asList("alexa");
    }

    public void startRecognition() {
        this.transcriberEnabled = true;
        recognizer.startRecognition(true);

        while (this.transcriberEnabled) {
            String utterance = recognizer.getResult().getHypothesis();
            for (String triggerWord : triggerWords) {
                if (utterance.equals(triggerWord)) {
                    this.transcriberListener.onSuccessfulTrigger();
                }
            }
        }
    }

    public void stopRecognition() {
        this.transcriberEnabled = false;
        recognizer.stopRecognition();
    }

    public boolean isListening() {
        return this.transcriberEnabled;
    }

    public void startRecording() {
    	this.transcriberEnabled = true;
    	recognizer.startRecording();
        while (this.transcriberEnabled) {
            String utterance = recognizer.getResult().getHypothesis();
            System.out.println("#####" + utterance);
            for (String triggerWord : triggerWords) {
                if (utterance.equals(triggerWord)) {
                    this.transcriberListener.onSuccessfulTrigger();
                }
            }
        }    	
    }
    
    public void stopRecording() {
    	this.transcriberEnabled = false;
    	recognizer.stopRecording();
    }      
    
}