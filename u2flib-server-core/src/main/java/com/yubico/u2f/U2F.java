/* Copyright 2014 Yubico */

package com.yubico.u2f;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.yubico.u2f.crypto.BouncyCastleCrypto;
import com.yubico.u2f.crypto.ChallengeGenerator;
import com.yubico.u2f.crypto.RandomChallengeGenerator;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.*;
import com.yubico.u2f.exceptions.U2fException;

import java.util.List;
import java.util.Set;

public class U2F {

    private final ChallengeGenerator challengeGenerator;
    private final U2fPrimitives primitives;

    public U2F() {
        this.challengeGenerator = new RandomChallengeGenerator();
        primitives = new U2fPrimitives(new BouncyCastleCrypto(), challengeGenerator);
    }

    /**
     * Initiates a high-level registration of a device, given a set of already registered devices.
     * @param appId the U2F AppID. Set this to the Web Origin of the login page, unless you need to
     *              support logging in from multiple Web Origins.
     * @param devices the devices currently registered to the user.
     * @return a RegisterRequestData, which should be sent to the client and temporarily saved by the server.
     */
    public RegisterRequestData startRegistration(String appId, Iterable<? extends DeviceRegistration> devices) {
        List<AuthenticateRequest> authenticateRequests = Lists.newArrayList();
        for(DeviceRegistration device : devices) {
            authenticateRequests.add(primitives.startAuthentication(appId, device));
        }
        return new RegisterRequestData(appId, devices, primitives, challengeGenerator);
    }

    public AuthenticateRequestData startAuthentication(String appId, Iterable<? extends DeviceRegistration> devices) throws U2fException {
        return new AuthenticateRequestData(appId, devices, primitives, challengeGenerator);
    }

    /***
     *
     */
    public DeviceRegistration finishRegistration(RegisterRequestData registerRequestData, RegisterResponse response) throws U2fException {
        return finishRegistration(registerRequestData, response, null);
    }

    /**
     * Finishes a previously started high-level registration.
     * @param registerRequestData the RegisterResponseData created by calling startRegistration
     * @param response The response from the device/client.
     * @param facets A list of valid facets to verify against.
     * @return a DeviceRegistration object, holding information about the registered device. Servers should
     * persist this.
     * @throws U2fException
     */
    public DeviceRegistration finishRegistration(RegisterRequestData registerRequestData, RegisterResponse response, Set<String> facets) throws U2fException {
        return primitives.finishRegistration(registerRequestData.getRegisterRequest(response), response, facets);
    }

    /**
     * @see U2F#finishAuthentication(com.yubico.u2f.data.messages.AuthenticateRequestData, com.yubico.u2f.data.messages.AuthenticateResponse, Iterable, java.util.Set)
     */
    public DeviceRegistration finishAuthentication(AuthenticateRequestData authenticateRequestData, AuthenticateResponse response, Iterable<? extends DeviceRegistration> devices) throws U2fException {
        return finishAuthentication(authenticateRequestData, response, devices, null);
    }

    /**
     * Finishes a previously started high-level authentication.
     * @param authenticateRequestData the AuthenticateRequestData created by calling startAuthentication
     * @param response                the response from the device/client.
     * @param devices                 the devices currently registered to the user.
     * @param facets                  A list of valid facets to verify against.
     * @return                        The (updated) DeviceRegistration that was authenticated against.
     * @throws U2fException
     */
    public DeviceRegistration finishAuthentication(AuthenticateRequestData authenticateRequestData, AuthenticateResponse response, Iterable<? extends DeviceRegistration> devices, Set<String> facets) throws U2fException {
        final AuthenticateRequest request = authenticateRequestData.getAuthenticateRequest(response);
        DeviceRegistration device = Iterables.find(devices, new Predicate<DeviceRegistration>() {
            @Override
            public boolean apply(DeviceRegistration input) {
                return Objects.equal(request.getKeyHandle(), input.getKeyHandle());
            }
        });

        primitives.finishAuthentication(request, response, device, facets);
        return device;
    }

}
