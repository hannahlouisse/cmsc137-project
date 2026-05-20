package model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WordDictionary {
	private static final List<String> WORD_DICTIONARY = Arrays.asList(
			"Computer Science", "Networking", "Languages", "Social Media",
			"Fruits", "Algorithm", "Applications", "Gaming",
			"Steam", "Filipino Food", "Animals on Water", "Programming", 
			"Disney Songs", "Singer Icons", "Michael Jackson", "Tradition", 
			"Pet Peeve", "Pedestrian Lane", "CHristmas", "School", "Summer",
			"Seasons", "Coffee", "Harry Potter", "Song Albums", "Road Trip",
			"Sem Starter/Ender", "Rush Hour", "Gas", "Graduation", "Sunscreen",
			"Makeup", "Birthday", "Vending Machine", "Trash Can", "Photo Booth",
			"Photography", "Amusement Park", "Aquarium", "Library", "Canteen",
			"Karaoke"
	);

	public static String getRandomWord() {
		Random rand = new Random();
		int wordIndex = rand.nextInt(WORD_DICTIONARY.size());
		return WORD_DICTIONARY.get(wordIndex);
	}
}