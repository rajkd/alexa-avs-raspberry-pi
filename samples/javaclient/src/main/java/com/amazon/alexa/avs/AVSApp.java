/**
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * You may not use this file except in compliance with the License. A copy of the License is located the "LICENSE.txt"
 * file accompanying this source. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.amazon.alexa.avs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.alexa.avs.auth.AccessTokenListener;
import com.amazon.alexa.avs.auth.AuthSetup;
import com.amazon.alexa.avs.auth.companionservice.RegCodeDisplayHandler;
import com.amazon.alexa.avs.config.DeviceConfig;
import com.amazon.alexa.avs.config.DeviceConfigUtils;
import com.amazon.alexa.avs.http.AVSClientFactory;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class AVSApp implements ExpectSpeechListener, RecordingRMSListener,
        RegCodeDisplayHandler, AccessTokenListener {

    private static final Logger log = LoggerFactory.getLogger(AVSApp.class);

    //JB dirty hack, paramterize these settings
    public static boolean DEV_MODE = false;


    private final AVSController controller;
    private Thread autoEndpoint = null; // used to auto-endpoint while listening
    private final DeviceConfig deviceConfig;
    // minimum audio level threshold under which is considered silence
    private static final int ENDPOINT_THRESHOLD = 3;
    private static final int ENDPOINT_SECONDS = 2; // amount of silence time before endpointing


    private AuthSetup authSetup;

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            new AVSApp(args[0]);
        } else {
            new AVSApp();
        }
    }

    public AVSApp() throws Exception {
        this(DeviceConfigUtils.readConfigFile());
    }

    public AVSApp(String configName) throws Exception {
        this(DeviceConfigUtils.readConfigFile(configName));
    }

    private AVSApp(DeviceConfig config) throws Exception {
        deviceConfig = config;
        controller = new AVSController(this, new AVSAudioPlayerFactory(), new AlertManagerFactory(),
                getAVSClientFactory(deviceConfig), DialogRequestIdAuthority.getInstance());

        //JB dirty hack, paramterize these settings
        if (!DEV_MODE) {
            authSetup = new AuthSetup(config, this);
            authSetup.addAccessTokenListener(this);
            authSetup.addAccessTokenListener(controller);
            authSetup.startProvisioningThread();
        }

        controller.startHandlingDirectives();
        addGPIOListener();

    }

    public void onSuccessfulTrigger() {
        controller.onUserActivity();

        RequestListener requestListener = new RequestListener() {
            @Override
            public void onRequestSuccess() {
                finishProcessing();
            }

            @Override
            public void onRequestError(Throwable e) {
                log.error("An error occured creating speech request", e);
                finishProcessing();
            }
        };

        final RecordingRMSListener rmsListener = this;
        this.controller.startRecording(rmsListener, requestListener);
    }
    
	private void addGPIOListener() {
		final GpioController gpio = GpioFactory.getInstance();

		final GpioPinDigitalInput myButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
		myButton.addListener(new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				if (event.getState().equals(PinState.HIGH)) {
					onSuccessfulTrigger();
				} else { // else we must already be in listening
					controller.stopRecording(); // stop the recording so the
												// request can complete
				}
			}
		});
	} 

    protected AVSClientFactory getAVSClientFactory(DeviceConfig config) {
        return new AVSClientFactory(config);
    }


    private void stopRecording() {
        controller.stopRecording();
    } 

    public void finishProcessing() {
        controller.processingFinished();
    }

    @Override
    public void rmsChanged(int rms) { // AudioRMSListener callback
        // if greater than threshold or not recording, kill the autoendpoint thread
        if ((rms == 0) || (rms > ENDPOINT_THRESHOLD)) {
            if (autoEndpoint != null) {
                autoEndpoint.interrupt();
                autoEndpoint = null;
            }
        } else if (rms < ENDPOINT_THRESHOLD) {
            // start the autoendpoint thread if it isn't already running
            if (autoEndpoint == null) {
                autoEndpoint = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(ENDPOINT_SECONDS * 1000);
                            stopRecording();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                };
                autoEndpoint.start();
            }
        }
    }

    @Override
    public void onExpectSpeechDirective() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                while (controller.isSpeaking()) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                }
                onSuccessfulTrigger();
            }
        };
        thread.start();
    }

    @Override
    public void displayRegCode(String regCode) {
        String regUrl =
                deviceConfig.getCompanionServiceInfo().getServiceUrl() + "/provision/" + regCode;
       /* showDialog("Please register your device by visiting the following website on "
                + "any system and following the instructions:\n" + regUrl
                + "\n\n Hit OK once completed.");*/
        System.out.println("Please register your device by visiting the following website on "
                + "any system and following the instructions:\n" + regUrl
                + "\n\n Hit OK once completed.");
    }

    @Override
    public synchronized void onAccessTokenReceived(String accessToken) {
    	System.out.println("Access Token Received: " + accessToken);
    }

}