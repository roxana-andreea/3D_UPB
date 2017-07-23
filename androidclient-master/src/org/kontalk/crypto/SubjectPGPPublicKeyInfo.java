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

package org.kontalk.crypto;

import java.io.IOException;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERBitString;


/**
 * A custom X.509 extension for a PGP public key.
 * @author Daniele Ricci
 */
public class SubjectPGPPublicKeyInfo extends ASN1Object {

    // based on UUID 24e844a0-6cbc-11e3-8997-0002a5d5c51b
    public static final String OID = "2.25.49058212633447845622587297037800555803";

    private DERBitString            keyData;

    public SubjectPGPPublicKeyInfo(ASN1Encodable publicKey) throws IOException {
        keyData = new DERBitString(publicKey);
    }

    public SubjectPGPPublicKeyInfo(byte[] publicKey) {
        keyData = new DERBitString(publicKey);
    }

    public DERBitString getPublicKeyData()
    {
        return keyData;
    }

    @Override
    public ASN1Primitive toASN1Primitive() {
        return keyData;
    }

}
