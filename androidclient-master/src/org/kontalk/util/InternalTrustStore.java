/*
 * Kontalk Android client
 * Copyright (C) 2014 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.kontalk.R;
import org.kontalk.crypto.PGP;

import android.content.Context;
import android.os.Build;


/** Some trust store utilities. */
public class InternalTrustStore {

    private static KeyStore sTrustStore;

    /**
     * Returns a trust store merged from the internal keystore and system
     * keystore.
     */
    public static KeyStore getTrustStore(Context context)
            throws KeyStoreException,
            NoSuchProviderException,
            NoSuchAlgorithmException,
            CertificateException,
            IOException {

        if (sTrustStore == null) {
            // load internal truststore from file
            sTrustStore = KeyStore.getInstance("BKS", PGP.PROVIDER);
            InputStream in = context.getResources()
                    .openRawResource(R.raw.truststore);
            sTrustStore.load(in, "changeit".toCharArray());

            // load system trust store
            KeyStore systemStore = loadSystemTrustStore();

            // copy system entries to our trust store
            Enumeration<String> aliases = systemStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = systemStore.getCertificate(alias);

                if (sTrustStore.containsAlias(alias))
                    alias = "system_" + alias;

                sTrustStore.setCertificateEntry(alias, cert);
            }
        }

        return sTrustStore;
    }

    /** Loads the system trust store. */
    private static KeyStore loadSystemTrustStore()
            throws KeyStoreException,
            NoSuchAlgorithmException,
            CertificateException,
            IOException {

        KeyStore ks;

        if (Build.VERSION.SDK_INT >= 14) {
            ks = KeyStore.getInstance("AndroidCAStore");
            ks.load(null, null);
        }

        else {
            ks = KeyStore.getInstance("BKS");
            String path = System.getProperty("javax.net.ssl.trustStore");
            if (path == null)
            path = System.getProperty("java.home") + File.separator + "etc"
                + File.separator + "security" + File.separator
                + "cacerts.bks";
            ks.load(new FileInputStream(path), null);
        }

        return ks;
    }
}
