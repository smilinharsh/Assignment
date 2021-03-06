package com.company.java;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

public class ReadPhrase {

	private static ReadPhrase instance;

	public static ReadPhrase getInstance(final String inputFilename) throws FileNotFoundException {
		if (instance == null) {
			instance = new ReadPhrase(inputFilename);
		} else {
			if (instance.isFree()) {
				instance = new ReadPhrase(inputFilename);
			} else {
				throw new IllegalStateException("Instance is not free to process another file");
			}
		}

		return instance;
	}

	private boolean free;
	private boolean busy;

	private File input;
	private File output;

	private Map<String, String> phrases;
	private Map<String, Integer> counter;
	private Map<String, Integer> result;

	private ReadPhrase(final String inputFilename) throws FileNotFoundException {
		if (inputFilename == null) {
			throw new IllegalArgumentException("inputFilename == null");
		}

		input = new File(inputFilename);

		if (input.exists() == false) {
			throw new FileNotFoundException("inputFilename does not exists! [" + inputFilename + "]");
		}

		setFree(true);
		setBusy(false);
	}

	public synchronized void processMostFrequentPhrases(final String outputFilename, final int n)
			throws FileAlreadyExistsException {
		if (isBusy() == true) {
			throw new IllegalStateException("instance is busy or not valid to process a file");
		} else if (outputFilename == null) {
			throw new IllegalArgumentException("outputFilename == null");
		} else if (outputFilename.trim() == "") {
			throw new IllegalArgumentException("outputFilename is empty");
		} else if (n < 1) {
			throw new IllegalArgumentException("n most frequent phrases must be greater then 0");
		}

		output = new File(outputFilename);

		if (output.exists()) {
			throw new FileAlreadyExistsException(outputFilename);
		}

		setBusy(true);

		readPhrases();
		processPhrases(n);
		writePhrasesToOutput();

		setBusy(false);
	}

	public void releaseInstance() {
		if (isFree() || isBusy()) {
			throw new IllegalStateException("instance is already free or is busy");
		}

		setFree(true);
		setBusy(false);
	}

	public boolean isFree() {
		return (free == true);
	}

	private void setFree(final boolean free) {
		this.free = free;
	}

	public boolean isBusy() {
		return (busy == true);
	}

	private void setBusy(final boolean busy) {
		this.busy = busy;
	}

	private void readPhrases() {
		phrases = new HashMap<String, String>();
		counter = new HashMap<String, Integer>();

		BufferedReader br = null;

		try {
			br = new BufferedReader(new FileReader(input));
			for (String line; (line = br.readLine()) != null;) {
				StringTokenizer tokenizer = new StringTokenizer(line, "|");

				while (tokenizer.hasMoreTokens()) {
					String phrase = tokenizer.nextToken();
					String key = Helper.getMd5Hash(phrase);

					if (phrases.containsKey(key)) {
						int count = counter.get(key);
						counter.put(key, ++count);
					} else {
						phrases.put(key, phrase);
						counter.put(key, 1);
					}
				}
			}
		} catch (IOException e) {
			throw new IllegalAccessError("Error reading input [" + input.getName() + "]");
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

	}

	private void processPhrases(final int n) {
		result = new LinkedHashMap<String, Integer>();

		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(counter.entrySet());

		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			public int compare(final Entry<String, Integer> o1, final Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		int count = 0;
		int last = 0;

		for (Entry<String, Integer> entry : list) {
			if (count >= n) {
				if (entry.getValue() < last) {
					break;
				}
			}

			result.put(entry.getKey(), entry.getValue());
			last = entry.getValue();
			count++;
		}
	}

	private void writePhrasesToOutput() {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(output));
			for (String key : result.keySet()) {
				String line = String.format("(%d)\t\t\t %s\n", result.get(key), phrases.get(key));
				bw.write(line);
			}
		} catch (IOException ioe) {
			throw new IllegalAccessError(ioe.getMessage());
		} finally {
			try {
				bw.close();
			} catch (Exception e) {
			}
		}

	}
}
