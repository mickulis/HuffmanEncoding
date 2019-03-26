/**
 * 	Autor: Michał Kulis
 *
 * 	Zakres projektu:
 *
 * 		UWAGA 1: bezwzględne ścieżki do plików
 * 		>>FIXED<<: UWAGA 2: wyjściowy plik .huff zakończony jest zerami dopełniającymi ostatni bajt do 8 bitów, co może spowodować dodanie 1~7 dodatkowych znaków na końcu pliku dekodowanego, jeśli jakiś znak posiada kod złożony z 1~7 zer
 *		UWAGA 3: w przypadku próby kompresji pustego pliku program wyrzuca wyjątek
 *		UWAGA 4: brak zabezpieczeń w przypadku próby dekompresji niepoprawnie sformatowanego/pustego pliku.
 *
 * 		Zadanie 1+2: kodowanie po jednym bajcie
 * 			pobiera dowolny plik,
 * 			wyświetla tabelę | (dec)bajt | liczba wystąpień | kod huffmana |
 * 			wyświetla przybliżoną wielkość pliku wejściowego i wyjściowego (wliczając nagłówek z drzewem huffmana)
 * 			kompresuje plik wejściowy do pliku .huff
 *
 *		Zadanie 3:
 *			rozpakowuje skompresowany wcześniej plik
 *
 */




import java.io.*;
import java.util.*;


public class HuffmanEncoder
{
	Fork treeRoot;
	
	InputStream inputStream;
	OutputStream outputStream;
	
	HuffmanEncoder() {
	
	
	}
	
	
	void Encode(String filePath, String destinationPath) throws IOException
	{
		/**
		 * Create input stream	/ done
		 */
		inputStream = new FileInputStream(filePath);
		
		/**
		 * Count character occurences	/ done
		 */
		HashMap<Integer, Integer> charCount = countCharacterOccurences();
		inputStream.close();
		
		
		/**
		 * Create priority queue with leaf nodes	/ done
		 */
		PriorityQueue<NodeWeight> nodeQueue = createPriorityQueue(charCount);
		
		
		
		/**
		 * Build a tree	/done
		 */
		treeRoot = buildTree(nodeQueue);
		
		
		/**
		 * Create output stream	/ done
		 */
		outputStream = new FileOutputStream(destinationPath, false);
		
		/**
		 * Write tree
		 */
		ArrayList<Integer> characterList = new ArrayList<>();
		BitWriter bitWriter = new BitWriter();
		treeRoot.initiateWritingTree(bitWriter, outputStream, characterList);
		if(!bitWriter.isEmpty())
		{
			outputStream.write(bitWriter.get());
		}
		
		for(int i = 0; i < characterList.size(); i++)
		{
			outputStream.write(characterList.get(i));
		}
		
		
		/**
		 * Create code map
		 */
		HashMap<Integer, String> codeMap = new HashMap<>();
		buildCodeMap(codeMap);
		
		
		/**
		 * Wypisanie listy (bajt, wystąpienia, kod)
		 * Oszacowanie rozmiaru pliku nieskompresowanego i skompresowanego
		 */
		
		printCodemap(codeMap, charCount);
		
		estimateFilesizes(codeMap, charCount);
		
		
		
		/**
		 * Create input stream	/ done
		 */
		inputStream = new FileInputStream(filePath);
		
		
		/**
		 * Encode and write each character	/ done
		 */
		
		int charToEncode = inputStream.read();
		
		bitWriter = new BitWriter();
		bitWriter.reset();
		
		String code;
		
		do
		{
			code = codeMap.get(charToEncode);
			for(int i = 0; i < code.length(); i++)
			{
				boolean one = (code.charAt(i) == '1');
				
				if(bitWriter.write(one))
				{
					//System.out.println(bitWriter.get());
					outputStream.write(bitWriter.get());
					bitWriter.reset();
				}
			}
			charToEncode = inputStream.read();
		}
		while(charToEncode >= 0);
		
		int currentPositionValue = bitWriter.getCurrentPositionValue();
		if(currentPositionValue > 0)
		{
			String endingCode = codeMap.get(256);	// wykorzystanie kodu na bajt zerowy do uzupełnienia ostatniego bajtu pliku.
			int unusedBits = 0;
			
			while(currentPositionValue > 0)
			{
				unusedBits++;
				currentPositionValue /= 2;
			}
			int iterator = 0;
			
			for(int i=0; i< unusedBits; i++)
			{
				bitWriter.write(endingCode.charAt(iterator) == '1');
				iterator++;
				iterator = iterator % endingCode.length();
			}
			outputStream.write(bitWriter.get());
		}
		
		outputStream.close();
	}
	
	private String padLeft(String string, int stringLength)
	{
		if(string.length() >= stringLength)
			return string.substring(0, stringLength);
		
		StringBuilder stringBuilder = new StringBuilder(string);
		
		for(int currentLength = string.length(); currentLength < stringLength; currentLength++)
		{
			stringBuilder.append(" ");
		}
		return stringBuilder.toString();
		
	}
	
	private void printCodemap(HashMap<Integer, String> codeMap, HashMap<Integer, Integer> charCount)
	{
		int keyPadding = 10;
		int occurencePadding = 10;
		int codePadding = 20;
		
		System.out.println(padLeft("KEY", keyPadding) + " | " + padLeft("COUNT", occurencePadding) + " | " + padLeft("CODE", codePadding));
		
		Map<Integer, String> sortedMap = new TreeMap<Integer, String>(codeMap);
		for(Map.Entry<Integer, String> entry : sortedMap.entrySet())
		{
			int key = entry.getKey();
			int occurences = charCount.get(key);
			String code = entry.getValue();
			System.out.println(padLeft(Integer.toString(key), keyPadding) + " | " + padLeft(Integer.toString(occurences), occurencePadding) + " | " + padLeft(code, codePadding));
		}
	}
	
	private void estimateFilesizes(HashMap<Integer, String> codeMap, HashMap<Integer, Integer> charCount)
	{
		long txtSize = 0;
		long huffSize = 0;
		int distinctBytesCount = 0;
		
		for(Map.Entry<Integer, String> entry : codeMap.entrySet())
		{
			distinctBytesCount++;
			int key = entry.getKey();
			int occurences = charCount.get(key);
			int codeLength = entry.getValue().length();
			
			txtSize += occurences * 8;
			huffSize += occurences * codeLength;
		}
		if(huffSize % 8 > 0)						// ...
		{											// ...
			huffSize = huffSize + 8 - huffSize % 8;	// ...zaokrąglony do pełnego bajtu
		}
		
		
		int treeSize = 2 * distinctBytesCount - 2;	// rozmiar zakodowanej topologii drzewa
		if(treeSize % 8 > 0)						// zaokrąglony do pełnego bajtu
		{											// drzewo binarne o n liściach ma 2n-1 węzłów, ale ignorujemy korzeń, więc uzyskujemy 2n-2
			treeSize = treeSize + 8 - treeSize % 8;	//
		}
		int nodeValues = distinctBytesCount * 8;	// suma rozmiarów wartości znajdujących się w liściach drzewa
		int huffHeader = treeSize + nodeValues;		// całkowity rozmiar nagłówka pliku
		
		System.out.println(
				"\nEstimated input file size: " + txtSize/8 + " bytes\n" +
				"Estimated .huff file size: " + (huffHeader + huffSize)/8 + " bytes");
	}
	
	
	
	
	
	
	PriorityQueue<NodeWeight> createPriorityQueue(HashMap<Integer, Integer> charCount)
	{
		PriorityQueue<NodeWeight> nodeQueue = new PriorityQueue<>();
		for(Map.Entry<Integer, Integer> entry : charCount.entrySet())
		{
			Leaf newLeaf = new Leaf(entry.getKey());
			NodeWeight nodeWeight = new NodeWeight(newLeaf, entry.getValue());
			nodeQueue.add(nodeWeight);
		}
		
		return nodeQueue;
	}
	
	HashMap<Integer, Integer> countCharacterOccurences() throws IOException
	{
		int nextByte;
		HashMap<Integer, Integer> charCount = new HashMap<>();
		nextByte = inputStream.read();
		if(nextByte < 0)
			throw new IOException("INPUT FILE IS EMPTY");
		do
		{
			charCount.putIfAbsent(nextByte, 0);
			charCount.put(nextByte, charCount.get(nextByte) + 1);
			nextByte = inputStream.read();
		}
		while(nextByte >= 0);
		
		charCount.put(256, 0);		// rozwiązanie problemu z UWAGI 2 - 256 jest zerem po zapisaniu tylko najmłodszego bitu, a jednocześnie nie koliduje z potencjalnym NULLem/bajtem zerowym, a liczba wystąpień = 0 zapewnia, że długość tego kodu będzie największa -> wykorzystany pod koniec metody Encode
		
		return charCount;
	}
	
	
	void buildCodeMap(HashMap<Integer, String> codeMap)
	{
		treeRoot.buildCharMap(new StringBuilder(), codeMap);
	}
	
	Fork buildTree(PriorityQueue<NodeWeight> nodeQueue)
	{
		NodeWeight left, right;
		while(nodeQueue.size() > 2)
		{
			left = nodeQueue.remove();
			right = nodeQueue.remove();
			
			//System.out.println("L: " + left.getNode().getValue() + " weight: " + left.getWeight() + " ; R: " + right.getNode().getValue() + " weight: " + right.getWeight());
			
			
			Fork newNode = new Fork(left.getNode(), right.getNode());
			int newWeight = left.getWeight() + right.getWeight();
			NodeWeight newNodeWeight = new NodeWeight(newNode, newWeight);
			nodeQueue.add(newNodeWeight);
		}
		
		Fork root = new Fork(nodeQueue.remove().getNode(), nodeQueue.remove().getNode());
		
		return root;
	}
	
	Fork replicateTree() throws IOException
	{
		boolean ongoing = true;
		BitReader bitReader = new BitReader();
		LinkedList<Fork> forkStack = new LinkedList<>();
		LinkedList<Leaf> leafQueue = new LinkedList<>();
		Fork currentFork = new Fork();
		Fork root = currentFork;
		
		int iterator = 1;
		
		while(ongoing)
		{
			bitReader.set(inputStream.read());
			
			for(int i=0; i < 8 && ongoing; i++)
			{
				boolean bit = bitReader.next();
				
				if(bit)
				{
					//System.out.println("1");
					Leaf newLeaf = new Leaf(0);
					leafQueue.add(newLeaf);
					
					
					if(currentFork.hasLeft())
					{
						//System.out.println(currentFork.getValue() + ": Current fork has left");
						currentFork.setRight(newLeaf);
						
						if(forkStack.size() == 0)
						{
							//System.out.println("Stack = 0, ending");
							ongoing = false;
						}
						else
						{
							currentFork = forkStack.pop();
							//System.out.println("Stack > 0, popping fork with number " + currentFork.getValue());
						}
					}
					else
					{
						//System.out.println(currentFork.getValue() + ": Current fork doesn't have left");
						currentFork.setLeft(newLeaf);
					}
				}
				else
				{
					//System.out.println("0");
					Fork newFork = new Fork();
					newFork.setValue(iterator++);
					if(currentFork.hasLeft())
					{
						//System.out.println(currentFork.getValue() + ": Current fork has left");
						currentFork.setRight(newFork);
					}
					else
					{
						//System.out.println(currentFork.getValue() + ": Current fork doesn't have left");
						currentFork.setLeft(newFork);
						forkStack.push(currentFork);
					}
					currentFork = newFork;
				}
			}
		}
		
		while(leafQueue.size() > 0)
		{
			leafQueue.remove().setValue(inputStream.read());
		}
		return root;
		
	}
	
	
	
	void setInputStream(InputStream stream)	// for testing only
	{
		inputStream = stream;
	}
	
	void setOutputStream(OutputStream stream)	// for testing only
	{
		outputStream = stream;
	}
	
	
	
	void Decode(String filePath, String destinationPath) throws IOException
	{
		
		inputStream = new FileInputStream(filePath);
		outputStream = new FileOutputStream(destinationPath);
		
		/**
		 * Recreate tree
		 */
		
		treeRoot = replicateTree();
		
		treeRoot.check();
		
		
		/**
		 * Decode
		 */
		
		BitReader bitReader = new BitReader();
		Node currentNode = treeRoot;
		
		
		while(bitReader.set(inputStream.read()))
		{
			for(int i = 0; i < 8; i++)
			{
				if(bitReader.next())
				{
					//System.out.println("right");
					currentNode = currentNode.getRight();
				}
				else
				{
					//System.out.println("left");
					currentNode = currentNode.getLeft();
				}
				
				if(currentNode instanceof Leaf)
				{
					//System.out.println("found " + currentNode.getValue());
					outputStream.write(currentNode.getValue());
					currentNode = treeRoot;
				}
			}
		}
		
		inputStream.close();
		outputStream.close();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	interface Node
	{
		
		void writeTree(BitWriter bitWriter, OutputStream fileOutputStream, ArrayList<Integer> characterList) throws IOException;
		void buildCharMap(StringBuilder stringBuilder, HashMap<Integer, String> charMap);
		int getValue();
		
		void setValue(int value);
		void setLeft(Node node);
		void setRight(Node node);
		
		Node getRight();
		Node getLeft();
		void check();
	}
	
	class Fork implements Node
	{
		Node left;
		Node right;
		int value = 0;
		
		Fork(){}
		Fork(Node left, Node right)
		{
			this.left = left;
			this.right = right;
		}
		
		public int getValue()
		{
			return value;
		}
		
		public void initiateWritingTree(BitWriter bitWriter, OutputStream fileOutputStream, ArrayList<Integer> characterList) throws IOException
		{
			left.writeTree(bitWriter, fileOutputStream, characterList);
			right.writeTree(bitWriter, fileOutputStream, characterList);
		}
		
		public void writeTree(BitWriter bitWriter, OutputStream fileOutputStream, ArrayList<Integer> characterList) throws IOException
		{
			if(bitWriter.write(false))
			{
				fileOutputStream.write(bitWriter.get());
				bitWriter.reset();
			}
			
			left.writeTree(bitWriter, fileOutputStream, characterList);
			right.writeTree(bitWriter, fileOutputStream, characterList);
		}
		
		
		public void buildCharMap(StringBuilder stringBuilder, HashMap<Integer, String> charMap)
		{
			StringBuilder leftStringBuilder = new StringBuilder(stringBuilder);
			leftStringBuilder.append('0');
			left.buildCharMap(leftStringBuilder, charMap);
			stringBuilder.append('1');
			right.buildCharMap(stringBuilder, charMap);
		}
		
		public void setValue(int value){this.value = value;}
		public void setLeft(Node node)
		{
			left = node;
		}
		public void setRight(Node node)
		{
			right = node;
		}
		
		public boolean hasLeft()
		{
			return (left != null);
		}
		
		public boolean hasRight()
		{
			return (right != null);
		}
		
		public Node getRight()
		{
			return right;
		}
		public Node getLeft()
		{
			return left;
		}
		
		
		public void check()
		{
			if(left == null)
			{
				System.out.println(value + ": LEFT IS NULL");
			}
			else
			{
				left.check();
			}
			if(right == null)
			{
				System.out.println(value + ": RIGHT IS NULL");
			}
			else
			{
				right.check();
			}
			
			
		}
		
	}
	
	class Leaf implements Node
	{
		int value;
		Leaf(int value)
		{
			this.value = value;
		}
		
		public int getValue()
		{
			return value;
		}
		
		public void writeTree(BitWriter bitWriter, OutputStream fileOutputStream, ArrayList<Integer> characterList) throws IOException
		{
			if(bitWriter.write(true))
			{
				fileOutputStream.write(bitWriter.get());
				bitWriter.reset();
			}
			characterList.add(value);
		}
		
		public void buildCharMap(StringBuilder stringBuilder, HashMap<Integer, String> charMap)
		{
			charMap.put(value, stringBuilder.toString());
		}
		
		public void setValue(int value)
		{
			this.value = value;
		}
		public void setLeft(Node node){}
		public void setRight(Node node){}
		
		public Node getRight()
		{
			return null;
		}
		
		public Node getLeft()
		{
			return null;
		}
		
		public void check(){}
	}
	
	class NodeWeight implements Comparable<NodeWeight>
	{
		Node node;
		int weight;
		
		
		NodeWeight(Node node, int weight)
		{
			this.node = node;
			this.weight = weight;
		}
		
		@Override
		public int compareTo(NodeWeight o)
		{
			return weight - o.weight;
		}
		
		public int getWeight()
		{
			return weight;
		}
		
		public Node getNode()
		{
			return node;
		}
	}
	
	
	public static void main(String[] args)
	{
		String toEncodeFilePath = "C:\\Users\\MK\\IdeaProjects\\HuffguyEncoding\\src\\input.txt";
		String encodedFilePath = "C:\\Users\\MK\\IdeaProjects\\HuffguyEncoding\\src\\output.huff";
		
		String toDecodeFilePath = "C:\\Users\\MK\\IdeaProjects\\HuffguyEncoding\\src\\output.huff";
		String decodedFilePath = "C:\\Users\\MK\\IdeaProjects\\HuffguyEncoding\\src\\outputDecoded.txt";
		
		
		HuffmanEncoder encoder = new HuffmanEncoder();
		try
		{
			encoder.Encode(toEncodeFilePath, encodedFilePath);
		}
		catch (IOException exception)
		{
			System.err.println(exception);
		}
		
		encoder = new HuffmanEncoder();	// nowy obiekt, aby udowodnić, że żadne dane z procesu kompresjii nie zostały wykorzystane przy dekompresji.
		try
		{
			encoder.Decode(toDecodeFilePath, decodedFilePath);
		}
		catch (IOException exception)
		{
			System.err.println(exception);
		}
		
	}
}






