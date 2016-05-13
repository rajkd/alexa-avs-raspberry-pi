package com.amazon.alexa.avs.speech;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;

public class Transcriber extends Thread {

	private static final Logger log = LoggerFactory.getLogger(Transcriber.class);
	
    private Configuration configuration;
    private LiveSpeechRecognizer recognizer;
    private final ClassLoader resLoader;
    private TranscriberListener transcriberListener;
    private boolean transcriberEnabled = false;
    private List<String> triggerWords;

    private static final String ACOUSTIC_MODEL = "res/en-us/";
    private static final String DICTIONARY_PATH = "res/cmudict-en-us.dict";
    private static final String GRAMMAR_PATH = "res/dialog/";
    private static final String LANGUAGE_MODEL = "res/en-us.lm.bin";
    private static final String GRAMMAR_NAME = "start";
    

    public Transcriber() {   
        configuration = new Configuration();
        resLoader = Thread.currentThread().getContextClassLoader();

        //URL url = resLoader.getResource(GRAMMAR_PATH);
    
        configuration.setAcousticModelPath(resLoader.getResource(ACOUSTIC_MODEL).toString());
        configuration.setDictionaryPath(resLoader.getResource(DICTIONARY_PATH).toString());
        configuration.setLanguageModelPath(resLoader.getResource(LANGUAGE_MODEL).toString());
        configuration.setGrammarPath(resLoader.getResource(GRAMMAR_PATH).toString());
        configuration.setUseGrammar(true);
        configuration.setGrammarName(GRAMMAR_NAME);

        try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException e) {
			log.error("An error occured creating Live Speech Recognizer", e);
		}

        this.triggerWords = Arrays.asList("skywalker", "alexa");
    }
    
    public void addListener(final TranscriberListener listener){
    	this.transcriberListener = listener;
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
}
