package com.akhil.fingerprint;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Tag";
    FingerprintManager managerFingerPrint;
    FingerprintManager.CryptoObject cryptoObject;
    FingerprintManager.AuthenticationCallback authCallback;
    CancellationSignal signal;
    private KeyguardManager keyguardManager;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private Cipher cipher;
    private String KEY_NAME = "Sample Key One";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        managerFingerPrint = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onCreate: No Permission Available");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_FINGERPRINT}, 4);

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        } else {
            if (managerFingerPrint.isHardwareDetected()) {
                Log.d(TAG, "onCreate: Hardware Detect");
                if (managerFingerPrint.hasEnrolledFingerprints()) {
                    Log.d(TAG, "onCreate: Enrolled FingerPrint");
                    keyguardManager =
                            (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                    try {
                        keyStore = KeyStore.getInstance("AndroidKeyStore");
                        keyGenerator = KeyGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_AES,
                                "AndroidKeyStore");
                        keyStore.load(null);
                        keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                                KeyProperties.PURPOSE_ENCRYPT |
                                        KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setUserAuthenticationRequired(true)
                                .setEncryptionPaddings(
                                        KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .build());
                        keyGenerator.generateKey();
                        if (cipherInit()) {
                            cryptoObject =
                                    new FingerprintManager.CryptoObject(cipher);
                            authCallback = new FingerprintManager.AuthenticationCallback() {
                                @Override
                                public void onAuthenticationError(int errorCode, CharSequence errString) {
                                    super.onAuthenticationError(errorCode, errString);
                                    Toast.makeText(MainActivity.this, "Failure", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                                    super.onAuthenticationSucceeded(result);
                                    Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                                    super.onAuthenticationHelp(helpCode, helpString);
                                    Toast.makeText(MainActivity.this, helpString, Toast.LENGTH_LONG).show();
                                }
                            };
                            signal = new CancellationSignal();
                            managerFingerPrint.authenticate(cryptoObject, signal, 0, authCallback, null);
                        }
                    } catch (KeyStoreException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (NoSuchProviderException e) {
                        e.printStackTrace();
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidAlgorithmParameterException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(MainActivity.this, "PermissonAvailable", Toast.LENGTH_SHORT).show();
        }

        //}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 4 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Permission Granted Result", Toast.LENGTH_SHORT).show();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            /*if (managerFingerPrint.isHardwareDetected()) {
                Log.d(TAG, "onRequestPermissionsResult: Hardware Detect");
                if (managerFingerPrint.hasEnrolledFingerprints()) {
                    Log.d(TAG, "onRequestPermissionsResult: Enrolled FingerPrint");
                    managerFingerPrint.authenticate();
                }
            }*/

        }
    }

    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }
}
