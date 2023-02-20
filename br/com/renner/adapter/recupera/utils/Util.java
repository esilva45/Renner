package br.com.renner.adapter.recupera.utils;

public class Util {
	public static String sanitizeString(String input) {
		// Accented characters to normal characters
		input = input.replaceAll("[áãàâä]", "a");
		input = input.replaceAll("[ÁÃÀÂÄ]", "A");
		input = input.replaceAll("[éèêë]", "e");
		input = input.replaceAll("[ÉÈÊË]", "E");
		input = input.replaceAll("[íìîï]", "i");
		input = input.replaceAll("[ÍÌÎÏ]", "I");
		input = input.replaceAll("[óõòôö]", "o");
		input = input.replaceAll("[ÓÕÒÔÖ]", "O");
		input = input.replaceAll("[úùûü]", "u");
		input = input.replaceAll("[ÚÙÛÜ]", "U");
		input = input.replaceAll("[ýÿ]", "y");
		input = input.replaceAll("[Ý]", "Y");
		input = input.replaceAll("[ç]", "c");
		input = input.replaceAll("[Ç]", "C");

		/* Remove special characters
		{
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < input.length(); i++) {
				if (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == ' ') {
					sb.append(input.charAt(i));
				}
			}

			input = sb.toString();
		}
		*/

		return input;
	}
}
