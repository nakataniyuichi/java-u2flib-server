/*
 * Copyright 2014 Yubico.
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file or at
 * https://developers.google.com/open-source/licenses/bsd
 */

package com.yubico.u2f.crypto;

import com.yubico.u2f.exceptions.U2fException;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

public class BouncyCastleCrypto implements Crypto {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static final String SIGNATURE_ERROR = "Error when verifying signature";
    public static final String ERROR_DECODING_PUBLIC_KEY = "Error when decoding public key";

    @Override
    public void checkSignature(X509Certificate attestationCertificate, byte[] signedBytes, byte[] signature)
            throws U2fException {
        checkSignature(attestationCertificate.getPublicKey(), signedBytes, signature);
    }

    @Override
    public void checkSignature(PublicKey publicKey, byte[] signedBytes, byte[] signature)
            throws U2fException {
        try {
            Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
            ecdsaSignature.initVerify(publicKey);
            ecdsaSignature.update(signedBytes);
            if (!ecdsaSignature.verify(signature)) {
                throw new U2fException("Signature is invalid");
            }
        } catch (InvalidKeyException e) {
            throw new U2fException(SIGNATURE_ERROR, e);
        } catch (SignatureException e) {
            throw new U2fException(SIGNATURE_ERROR, e);
        } catch (NoSuchAlgorithmException e) {
            throw new U2fException(SIGNATURE_ERROR, e);
        }
    }

    @Override
    public PublicKey decodePublicKey(byte[] encodedPublicKey) throws U2fException {
        try {
            X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
            ECPoint point;
            try {
                point = curve.getCurve().decodePoint(encodedPublicKey);
            } catch (RuntimeException e) {
                throw new U2fException("Could not parse user public key", e);
            }

            return KeyFactory.getInstance("ECDSA").generatePublic(
                    new ECPublicKeySpec(point,
                            new ECParameterSpec(
                                    curve.getCurve(),
                                    curve.getG(),
                                    curve.getN(),
                                    curve.getH()
                            )
                    )
            );
        } catch (InvalidKeySpecException e) {
            throw new U2fException(ERROR_DECODING_PUBLIC_KEY, e);
        } catch (NoSuchAlgorithmException e) {
            throw new U2fException(ERROR_DECODING_PUBLIC_KEY, e);
        }
    }

    @Override
    public byte[] hash(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Error when computing SHA-256", e);
        }
    }

    @Override
    public byte[] hash(String str) {
        return hash(str.getBytes());
    }
}
