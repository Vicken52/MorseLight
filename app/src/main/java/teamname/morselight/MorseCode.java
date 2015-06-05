package teamname.morselight;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ryan on 5/10/2015.
 *
 * Encode alphabet to MorseCode and decode Morsecode to alphabet
 * have to decide which to use: "-" or "_" for morse code
 * only decode a word at a time because there is no notation to signal new word yet
 * should only encode a sentence at most
 */
public class MorseCode {
    private static final String[] alphabets = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                                    "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
                                    "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", " "};
    private static  final String[] morses = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.",
                                    "....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.",
                                    "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-",
                                    "-.--", "--..", ".----", "..---", "...--", "....-", ".....",
                                    "-....", "--...", "---..", "----.", "-----", "/"};
    private static Map<String, String> ALPHABET_MAP, MORSE_MAP;

    // Initialize 2 maps
    static {
        ALPHABET_MAP = new HashMap<String, String>();
        MORSE_MAP = new HashMap<String, String>();
        for (int i = 0; i < alphabets.length; i++) {
            ALPHABET_MAP.put(alphabets[i], morses[i]);
            MORSE_MAP.put(morses[i], alphabets[i]);
        }
    }

    // encode alphabet string to morse code.
    // make it a static method to be called without creating an object
    public static String encode(String str){
        String result = "";
        str = str.toUpperCase();
        String tmp = "";
        if (str != null || !str.isEmpty())
            for (int i = 0; i < str.length(); i++){
                // check for A-Z and 0-9
                if ((str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') || (str.charAt(i) >= '0' && str.charAt(i) <= '9') || str.charAt(i) == ' '){
                    tmp = "" + str.charAt(i);	// must convert from char to String for map.get() method to work
                    //System.out.println("working on: " + tmp);
                    ///System.out.println("found in map: "+ ALPHABET_MAP.get(tmp));
                    //System.out.println("Test A: "+ ALPHABET_MAP.get("A"));
                    result += ALPHABET_MAP.get(tmp) + "   ";
                }
            }
        return result;
    }

    // decode
    // a space must be between each word. only translate a word
    public static String decode(String str){
        String result = "";
        if (str != null || !str.isEmpty()){
            String[] tmp = str.split("   ");
            for (String s: tmp)
                result +=MORSE_MAP.get(s);
        }
        return result;
    }

}
