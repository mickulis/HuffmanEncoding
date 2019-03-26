public class BitReader
{
	int byteValue;
	int position;
	
	boolean set(int i)
	{
		byteValue = i;
		position = 128;
		if(i < 0)
			return false;
		else
			return true;
	}
	
	boolean next()
	{
		if(position == 0)
			throw new RuntimeException();
		
		int intermediate = byteValue / position;
		position /= 2;
		
		return (intermediate % 2 == 1);
	}
	
	public static void main(String[] args)
	{
		BitReader bitReader = new BitReader();
		
		System.out.println("Non-negative value: " + bitReader.set(25));
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println(bitReader.next());
		System.out.println("Non-negative value: " + bitReader.set(-1));
		
		
	}
}

