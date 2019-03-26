public class BitWriter
{
	int byteValue = 0;
	int position = 128;
	boolean empty = true;
	
	boolean write(boolean bool)
	{
		if(bool)
			byteValue += position;
		position /= 2;
		
		empty = false;
		
		if(position == 0)
			return true;
		else
			return false;
	}
	
	int get()
	{
		return byteValue;
	}
	
	void reset()
	{
		byteValue = 0;
		position = 128;
		empty = true;
	}
	
	
	boolean isEmpty()
	{
		return empty;
	}
	
	int getCurrentPositionValue()
	{
		return position;
	}
	
	
	public static void main(String[] args)
	{
		BitWriter bitWriter = new BitWriter();
		
		bitWriter.write(true);
		System.out.println(bitWriter.get());
		bitWriter.write(true);
		System.out.println(bitWriter.get());
		
		bitWriter.write(true);
		System.out.println(bitWriter.get());
		
		bitWriter.write(true);
		System.out.println(bitWriter.get());
		
		bitWriter.write(true);
		System.out.println(bitWriter.get());
		
		
		
		
	}
}
