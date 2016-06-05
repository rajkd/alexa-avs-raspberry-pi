/**
 * @module
 * This module defines the settings that need to be configured for a new
 * environment.
 * The clientId and clientSecret are provided when you create
 * a new security profile in Login with Amazon.  
 * 
 * You will also need to specify
 * the redirect url under allowed settings as the return url that LWA
 * will call back to with the authorization code.  The authresponse endpoint
 * is setup in app.js, and should not be changed.  
 * 
 * lwaRedirectHost and lwaApiHost are setup for login with Amazon, and you should
 * not need to modify those elements.
 */
var config = {
    clientId: 'amzn1.application-oa2-client.606c609e6b1e43d6aec9a1938b184b74',
    clientSecret: '75a71382474cb2e0b58b893983eaad766a89055e7a0ffb7c7f42a3e1eb213412',
    redirectUrl: 'https://localhost:3000/authresponse',
    lwaRedirectHost: 'amazon.com',
    lwaApiHost: 'api.amazon.com',
    validateCertChain: true,
    sslKey: '/Users/rajkumar/git/alexa-avs-raspberry-pi/samples/javaclient/certs/server/node.key',
    sslCert: '/Users/rajkumar/git/alexa-avs-raspberry-pi/samples/javaclient/certs/server/node.crt',
    sslCaCert: '/Users/rajkumar/git/alexa-avs-raspberry-pi/samples/javaclient/certs/ca/ca.crt',
    products: {
        "RaspberryEcho": ["123456"], // Fill in with valid device values, eg: "testdevice1": ["DSN1234", "DSN5678"]
    },
};

module.exports = config;
