package ru.synthet.synthpass;
/*
 * Copyright 2013 Vladimir Synthet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.util.Log;
import iaik.sha3.IAIKSHA3Provider;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Security;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PassGenerator {

    private final String TAG = getClass().getName();

    private final static String baseUpperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final static String baseLowerCase = "abcdefghijklmnopqrstuvwxyz";
    private final static String baseDigits    = "0123456789";
    private MessageDigest sha512;
    private final Pattern noConsecutiveCharactersPattern;
    private final Pattern requireUppercaseLetters;
    private final Pattern requireLowercaseLettersPattern;
    private final Pattern requireDigitsPattern;
    private final Pattern requireSpecialSymbolsPattern;
    private final char[] symbolsArr;
    private final int symbolsLength;
    public static String algorithm = "vpass3";

    PassGenerator() {
        super();
        // prepare digest provider
        IAIKSHA3Provider provider = new IAIKSHA3Provider();
        Security.addProvider(provider);
        try {
            sha512 = MessageDigest.getInstance("KECCAK512", "IAIK_SHA3");
        } catch (Exception ex) {
            Log.e(TAG, "Error", ex);
        }
        // prepare all available letters, digits, symbols
        String baseSymbols = baseUpperCase + baseLowerCase + baseDigits;
        String symbols = baseSymbols + PassRules.availableSymbols;
        // convert available letters, digits, symbols into array
        symbolsArr = symbols.toCharArray();
        symbolsLength = symbolsArr.length;
        // init and compile regexp patterns
        noConsecutiveCharactersPattern = Pattern.compile("(.)\\1");
        requireUppercaseLetters        = Pattern.compile("[A-Z]");
        requireLowercaseLettersPattern = Pattern.compile("[a-z]");
        requireDigitsPattern           = Pattern.compile("\\d");
        String symbolRegex;
        StringBuilder sb = new StringBuilder();
        char[] symbolCharArr = PassRules.availableSymbols.toCharArray();
        int symbolCharLen = symbolCharArr.length;
        for (int i=0; i<symbolCharLen; i++) {
            if (i<symbolCharLen-1)
                sb.append("\\").append(symbolCharArr[i]).append("|");
            else
                sb.append("\\").append(symbolCharArr[i]);
        }
        symbolRegex = sb.toString();
        requireSpecialSymbolsPattern = Pattern.compile(symbolRegex);
    }

    static class PassRules {
        public static int generatedPasswordLength     = 12;
        public static final String availableSymbols         = "!#$%&()*,-.";
        public static boolean requireUppercaseLetters = true;
        public static boolean requireLowercaseLetters = true;
        public static boolean requireDigits           = true;
        public static boolean requireSpecialSymbols   = true;
        public static final boolean noConsecutiveCharacters = true;
    }

    String getSymbol(int num) {
        String base = "";
        if (PassRules.requireSpecialSymbols)
            base += PassRules.availableSymbols;
        if (PassRules.requireDigits)
            base += baseDigits;
        if (PassRules.requireLowercaseLetters)
            base += baseLowerCase;
        if (PassRules.requireUppercaseLetters)
            base += baseUpperCase;
        char[] baseArr = base.toCharArray();
        int baseLen = baseArr.length;
        if (baseLen > 0)
            return Character.toString(baseArr[num % baseLen]);
        else
            return "";
    }

// --Commented out by Inspection START (07.10.13 15:05):
//    protected String generate(String inputString, String domain) {
//        inputString = inputString + domain;
//        return encryptAndValidate(inputString);
//    }
// --Commented out by Inspection STOP (07.10.13 15:05)

    private String encryptAndValidate(String inputString) {
        String encrypted;
        do {
            encrypted = encrypt(inputString, PassRules.generatedPasswordLength);
            inputString += '.';
        } while (!validate(encrypted));
        return encrypted;
    }

    boolean validate(String inputString) {
        Matcher matcher;
        if (PassRules.noConsecutiveCharacters) {
            matcher = noConsecutiveCharactersPattern.matcher(inputString);
            if (matcher.find()) return false;
        }
        matcher = requireUppercaseLetters.matcher(inputString);
        if ((PassRules.requireUppercaseLetters) && !(matcher.find())) return false;
        if (!(PassRules.requireUppercaseLetters) && (matcher.find())) return false;
        matcher = requireLowercaseLettersPattern.matcher(inputString);
        if ((PassRules.requireLowercaseLetters) && !(matcher.find())) return false;
        if (!(PassRules.requireLowercaseLetters) && (matcher.find())) return false;
        matcher = requireDigitsPattern.matcher(inputString);
        if ((PassRules.requireDigits) && !(matcher.find())) return false;
        if (!(PassRules.requireDigits) && (matcher.find())) return false;
        matcher = requireSpecialSymbolsPattern.matcher(inputString);
        if ((PassRules.requireSpecialSymbols) && !(matcher.find())) return false;
        if (!(PassRules.requireSpecialSymbols) && (matcher.find())) return false;
        return true;
    }

    /*
    public static double calculateShannonEntropy(String inputString) {
        Map<Character, Integer> map = new HashMap<Character, Integer>();
        // count the occurrences of each value
        for (int i=0;i<inputString.length();i++) {
            Character sequence = inputString.charAt(i);
            if (!map.containsKey(sequence)) {
                map.put(sequence, 0);
            }
            map.put(sequence, map.get(sequence) + 1);
        }
        // calculate the entropy
        Double result = 0.0;
        for (Character sequence : map.keySet()) {
            Double frequency = (double) map.get(sequence) / inputString.length();
            result -= frequency * (Math.log(frequency) / Math.log(2));
        }

        return result;
    }
    */

    String getShakedString(Float entropy[]) {
        ByteBuffer buf = ByteBuffer.allocate(entropy.length*4);
        for (Float anEntropy : entropy) {
            buf.putFloat(anEntropy);
        }
        byte[] entropyBytes = buf.array();
        entropyBytes = hash(entropyBytes);
        String returnString;
        String str = "";
        int num = 0;
        char chr = 0;
        for (int i=0; i < entropy.length; i++) {
            // some black magic
            num = (3*i + 5*num + 7*chr)%entropyBytes.length;
            str = getSymbol(entropyBytes[num] & 0xFF);
            if (str.length() > 0)
                chr = str.charAt(0);
        }
        returnString = str;
        return returnString;
    }

    private byte[] hash(String inputString) {
        byte[] inputHashArr = new byte[]{};
        sha512.reset();
        try {
            inputHashArr = sha512.digest(inputString.getBytes("UTF-8"));
        } catch (Exception ex) {
            Log.e(TAG, "Error", ex);
        }
        return inputHashArr;
    }

    private byte[] hash(byte[] inputBytes) {
        byte[] inputHashArr = new byte[]{};
        sha512.reset();
        try {
            inputHashArr = sha512.digest(inputBytes);
        } catch (Exception ex) {
            Log.e(TAG, "Error", ex);
        }
        return inputHashArr;
    }

    String synthEncrypt(String inputString, int requiredLength) {
        byte[] inputHashArr = hash(inputString);
        String returnString = "";
        int num = 0;
        for (int i=0; i < requiredLength; i++) {
            num = (i + inputString.length() + num)%inputHashArr.length;
            String chr = getSymbol(inputHashArr[num] & 0xFF);
            if ((i > 0) && (returnString.charAt(i-1) == chr.charAt(0))) {
                i--;
                continue;
            }
            returnString += chr;

        }
        return returnString;
    }

    String encrypt(String inputString, int requiredLength) {
        int inputStringLength = inputString.length();
        byte[] inputHashArr = hash(inputString);
        // convert byte digest to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : inputHashArr) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String inputHash  = hexString.toString();
        int inputHashLength = inputHash.length();
        // Calculate the offset of the first character.
        int last = 0;
        if (inputStringLength > inputHashLength) inputStringLength = inputHashLength;
        for (int i = 0; i < inputStringLength; i++) {
            last = (inputHash.charAt(i) + 31 * last) % 59;
        }
        // Grow input_string if it's shorter than required length
        while (inputHashLength < requiredLength) {
            inputHash += inputHash;
            inputHashLength += inputHashLength ;
        }
        inputHash = inputHash.substring(0, requiredLength);
        // Generate the encrypted string.
        String returnString =  "";
        for(int i = 0; i < requiredLength; i++) {
            returnString += symbolsArr[last = (i + last + inputHash.charAt(i)) % symbolsLength];
        }
        return returnString;
    }
}
