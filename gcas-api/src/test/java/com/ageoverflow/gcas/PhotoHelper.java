package com.ageoverflow.gcas;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Replicates the Python Photo/Photos helper from the original test suite.
 * Encoding: base64( json_string( "complexity|age|silent|boomer|x|y|z|alpha|filler" ) )
 * Checksum:  "0x" + hex( sum of ages across all photos )
 */
class PhotoHelper {

    static String createPhoto(int complexity, int age, int silent, int boomer, int x, int y, int z, int alpha) {
        String filler = "Lorem ipsum dolor sit amet consectetur";
        String pipe = complexity + "|" + age + "|" + silent + "|" + boomer + "|"
                + x + "|" + y + "|" + z + "|" + alpha + "|" + filler;
        // json.dumps of a plain string just wraps it in double-quotes
        String json = "\"" + pipe + "\"";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Default photo used in create_default_analysis_request: age=29, y=60, z=40 */
    static String defaultPhoto() {
        return createPhoto(0, 29, 0, 0, 0, 60, 40, 0);
    }

    /** Python: hex(age) → e.g. "0x1d" for age 29 */
    static String checksum(int age) {
        return "0x" + Integer.toHexString(age);
    }

    /** Python Photos.checksum(): hex( sum of all ages ) */
    static String checksum(int... ages) {
        int total = 0;
        for (int a : ages) total += a;
        return "0x" + Integer.toHexString(total);
    }
}
