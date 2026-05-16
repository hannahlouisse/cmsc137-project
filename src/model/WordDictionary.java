package model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WordDictionary {
	private static final List<String> WORD_DICTIONARY = Arrays.asList(
			"CMSC137", "Networking", "Milestone", "Project",
			"Computer", "Algorithm", "Database", "Security",
			"JavaScript", "Python", "Java", "Programming"
	);

	public static String getRandomWord() {
		Random rand = new Random();
		int wordIndex = rand.nextInt(WORD_DICTIONARY.size());
		return WORD_DICTIONARY.get(wordIndex);
	}
}