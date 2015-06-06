import java.util.Scanner;

public class P2 {
	public static void main(String[] args) {
		// take input from stdin
		// use loop for multiple input lines
		System.out.println("Enter input(k pattern1 pattern2 XXXXXXXXXXXX): ");
		Scanner input = new Scanner(System.in);
		String inputString;
		String[] inputArray;
		
		while(input.hasNextLine()) {
			inputString = input.nextLine().trim();
			if(inputString.length() == 0 || inputString.charAt(0) == '#')
				continue;
			inputArray = inputString.split("\\s+");
			
			// validate parameters
			if(!validateParameters(inputArray)) {
				System.out.println("*********************************************"
						+ "****************************************");
				continue;
			}
			
			decoder(inputArray); // decode and print result for each line of input
			System.out.println("*********************************************"
					+ "****************************************");
		}
		input.close();
	}
	
	/* 	 
	 * Viterbi_Decoder implementation:
	 * a. parse input parameters
	 *    all states and output are converted from binary to integer
	 *    etc. 00->0, 01->1, 10->2, 11->3
	 * b. initialize the tables that are independent of input and output,
	 *    including nextStateTable, outputTable and inputTable
	 * c. create two tables to store route information (predecessor and accumulatedErrorMetrics)
	 * d. iteratively calculate predecessor and accumulated error metrics of next state 
	 *    using current state
	 * e. get input binary by tracing back from the end using predecessor table and the state with smallest error metrics
	 * f. compare the correct output from input with received, and locate transmission errors
	 */
	public static void decoder(String[] inputArray) {		
		int k = Integer.parseInt(inputArray[0]); // constraint length
		int c1 = Integer.parseInt(inputArray[1]); // XOR pattern of channel 1
		int c2 = Integer.parseInt(inputArray[2]); // XOR pattern of channel 2
		String recvString = inputArray[3]; // recvString is the received binary signal
		
		// parse received string to an array containing paired symbols
		int[] recvArray = getObservations(recvString);
		
		// initialize predefined tables that are independent on received binary string
		int[][] nextStateTable = getNextStateTable(k); // from current state to next state based on input 0 or 1
		int[][] outputTable = getOutputTable(k, c1, c2); // output of current state based on input 0 or 1
		int[][] inputTable = getInputTable(k); // input that convert current state to next state
		
		// create two tables that are dependent on received binary string to store route information
		int numOfRows = (int) Math.pow(2, k - 1); // number of states
		int numOfCols = recvString.length() / 2 + 1; // trace-back depth is the total binary string
		int[][] predecessor = new int[numOfRows][numOfCols]; // table to store state predecessor history up to k * 5 levels
		int[][] accumulatedErrorMetrics = new int[numOfRows][2]; // table to store accumulated error metrics for current and next state
		
		//initialize accumulated error metrics for every state
		accumulatedErrorMetrics[0][0] = 0; // set accumulated error metrics of starting state to 0 in every cycle
		accumulatedErrorMetrics[0][1] = Integer.MAX_VALUE / 2; // set to max_int/2 before calculation and to avoid overflow
		for(int i = 0; i < numOfRows; i++) {
			// set accumulated error metrics in the first column at t = 0 except the starting state to Integer.MAX_VALUE / 2
			// so trace-back only to state 0
			if(i != 0) {
				accumulatedErrorMetrics[i][0] = Integer.MAX_VALUE / 2; // non-start state
				accumulatedErrorMetrics[i][1] = Integer.MAX_VALUE / 2; // non-start state at next time instance
			}
		}
		
		/*
		 * the main loop has two tasks:
		 * a. find smallest accumulated error metrics for each state 
		 * b. find the predecessor of each state at each time instance
		 */
		int count = 0; // number of input processed
		while(count < recvArray.length) {
			// the outer loop do calculations for current state first using 0, then 1
			// the accumulate errors for current time instance are stored in column 0 of accumulatedErrorMetrics table
			// the accumulate errors for next time instance are stored in column 1 of accumulatedErrorMetrics table
			for(int j = 0; j < 2; j++) {
				// the inner loop uses current states to calculate next state
				// if two current states map to the same next state, take the route with smaller accumulated errors
				for(int i = 0; i < numOfRows; i++) {
					int nextState = nextStateTable[i][j];
					int output = outputTable[i][j]; // output with input 0 or 1 at state i
					int errorMetrics = getErrorMetrics(output, recvArray[count]); // error metric between anticipated output and received
					int newMetrics = accumulatedErrorMetrics[i][0] + errorMetrics; // accumulated error metric of next state
					if(newMetrics < accumulatedErrorMetrics[nextState][1]) {
						// change the accumulated error metrics and predecessor if the new calculation for the same next state is smaller
						accumulatedErrorMetrics[nextState][1] = newMetrics;
						predecessor[nextState][count + 1] = i; // predecessor of next state (at time count+1) is current state i
					}
				}
			}			
			
			// copy column 1 of accumulated error metrics table to column 0
			// reset column 1 to Integer.MAX_VALUE / 2 for calculation at next time instance
			for(int i = 0; i < numOfRows; i++) {
				accumulatedErrorMetrics[i][0] = accumulatedErrorMetrics[i][1];
				accumulatedErrorMetrics[i][1] = Integer.MAX_VALUE / 2;
			}	
			count++; // move to the next input
			
		}
		
		/*
		 * trace back and print out input binary
		 */
		// trace back from the end using predecessor table and accumulated error metrics
		int traceLen = count;
		int[] tracebackStates = getTrace(predecessor, accumulatedErrorMetrics, traceLen);
		
		// get the original binary string
		String result = getInputSegment(tracebackStates, inputTable);
		String resultNoFlushBits = result.substring(0, result.length() - k + 1);
		System.out.println("The original input sequence is: " + resultNoFlushBits + "\n");
		
		/*
		 * analysis of decoding:
		 * a. find correct output binary using input sequence
		 * b. compare correct output binary with received and find locations of errors
		 */
		// print out the differences between correct sequence and received one
		String correctSeq = getCorrectSeq(resultNoFlushBits, k, c1, c2);
		System.out.println("correct sequence:\t" + correctSeq);
		System.out.println("received sequence:\t" + recvString);
		
		// find the number of different bits between correct and corrupt sequence
		int[] mutations = findDiff(correctSeq, recvString);
		System.out.print("error (marked as \"x\"):\t");
		int numOfErrors = 0;
		for(int i = 0; i < mutations.length; i++)
			if(mutations[i] == 1) {
				System.out.print("x");
				numOfErrors++;
			}
			else System.out.print("-");
		System.out.println();
		if(numOfErrors == 0) // no error bits
			System.out.println("There're no errors in transmission");
		else {
			System.out.println("The received sequence has " + numOfErrors + (numOfErrors == 1? " error" : " errors"));
		}
	}
	
	// find the position of difference between two strings of same length
	public static int[] findDiff(String s1, String s2) {
		int[] arr = new int[s1.length()];
		for(int i = 0; i < s1.length(); i++)
			if(s1.charAt(i) != s2.charAt(i))
				arr[i] = 1;
		return arr;
	}
	
	//get correct convolutional codes for a input binary string
	public static String getCorrectSeq(String inputString, int k, int c1, int c2) {
		for(int i = 0; i < k - 1; i++)
			inputString += '0'; // add k-1 '0's to then end to flush the end
		
		int[][] nextStateTable = getNextStateTable(k);
		int[][] outputTable = getOutputTable(k, c1, c2);
		
		// starting from state 0
		int curState = 0;
		String result = ""; // store output
		for(int i = 0; i < inputString.length(); i++) {
			int curBit = inputString.charAt(i) - '0';
			int output = outputTable[curState][curBit];
			if(output == 0) result += "00";
			else if(output == 1) result += "01";
			else if(output == 2) result += "10";
			else result += "11";
			curState = nextStateTable[curState][curBit];
		}
		
		return result;
	}
	
	// calculate the binary input segment based on the segment's trace-back information
	public static String getInputSegment(int[] list, int[][] inputTable) {
		String segment = "";
		for(int i = 0; i < list.length - 1; i++) {
			segment += inputTable[list[i]][list[i+1]];
		}
		
		return segment;
	}
	
	// calculate the list of states for the route with smallest accumulated error metrics from trace-back
	public static int[] getTrace(int[][] predecessor, int[][] accumulatedErrorMetrics, int len) {
		int[] list = new int[len + 1];
		
		// find the smallest accumulated error metric state at the end to start trace-back
		int end = 0;
		for(int i = 1; i < accumulatedErrorMetrics.length; i++) {
			if(accumulatedErrorMetrics[i][0] < accumulatedErrorMetrics[end][0])
				end = i;
		}
		
		list[len] = end;
		// trace back to the starting point
		for(int i = len - 1; i >= 0; i--) {
			list[i] =  predecessor[list[i + 1]][i + 1];
		}
		
		return list;
	}
	
	// calculate error metrics between two pairs of channel symbols
	public static int getErrorMetrics(int output, int received) {
		int errorMetrics = 0;
		for(int i = 0; i < 2; i++) {
			if(output % 2 != received % 2) errorMetrics++;
			output /= 2;
			received /= 2;
		}
		return errorMetrics;
	}
	
	/*	 validate input parameters
	 * a. one input must have four parameters
	 * b. must be all digits
	 * c. constraint length must be >= 2
	 * d. pattern integers must be between 2^k - 1 and 1 (inclusive)
	 * e. received binary must have even number of digits containing only 0s and 1s
	 * f. received binary length must be >= 2*k
	 */
	public static boolean validateParameters(String[] args) {
		System.out.print("You entered: ");
		for(int i = 0; i < args.length; i++) // print out input
			System.out.print(args[i] + "--");
		System.out.println();
		
		if(args.length != 4) { // number of argument is not 4
			System.out.println("Invalid number of input parameters: " + args.length);
			return false;
		}
		
		int k = 0, c1 = 0, c2 = 0; // k is constraint length, c1 and c2 is integer representation of xor pattern of channel 1 and 2
		String recvString = "";
		// check input of constraint length, channel 1 and channel 2 patterns
		try {
			k = Integer.parseInt(args[0]);
			c1 = Integer.parseInt(args[1]);
			c2 = Integer.parseInt(args[2]);
		}
		catch(Exception e) {
			System.out.println("Invalid input: first three parameters must be integers");
			return false;
		}
		
		// check constraint length
		if(k < 2) {
			System.out.println("Invalid constraint length: must >= 2");
			return false;
		}
		// the integer of channel XOR pattern must be no larger than 2^k - 1 and no smaller than 1
		if(Math.max(c1, c2) > (int) Math.pow(2, k) - 1 || Math.min(c1, c2) < 1) {
			System.out.println("Invalid channel XOR pattern: must >= 1 and <= 2^k - 1");
			return false;
		}
		// 
		if(c1 == c2) {
			System.out.println("Duplicate XOR pattern");
			return false;
		}
		
		// validate received binary string
		recvString = args[3];
		if(recvString.length() < 2 * k || recvString.length() % 2 != 0) {
			System.out.println("Invalid received binary string: length must be even and no smaller than 2*k");
			return false;
		}
		// check if it's a binary string
		for(int i = 0; i < recvString.length(); i++)
			if(recvString.charAt(i) != '0' && recvString.charAt(i) != '1') {
				System.out.println("Invalid received binary string: must contain only 0 and 1");
				return false;
			}
		
		return true;
	}
	
	// received result array
	// parse observed output binary string into channel results at each time point
	public static int[] getObservations(String s) {
		int[] obs = new int[s.length() / 2];
		for(int i = 0; i < s.length() / 2; i++)
			obs[i] = Integer.parseInt(s.substring(2 * i, 2 * i + 2), 2); // binary string to integer
		return obs;
	}
	
	/*
	 * all states and output symbol pairs are converted from binary to integer
	 * etc. 00->0, 01->1, 10->2, 11->3
	 */
	// next state table, row x column = 2^(k - 1) x 2:
	// construct the table of next states VS current states with a input of 0 or 1 
	// k is the constraint length, k - 1 is the number of registers
	public static int[][] getNextStateTable(int k) {
		int numOfRows = (int) Math.pow(2, k - 1);
		int[][] nextStateTable = new int[numOfRows][2];
		
		// nextStateTable[][0] is next state with input = 0
		// nextStateTable[][1] is next state with input = 1
		for(int i = 0; i < numOfRows; i++) {
			nextStateTable[i][0] = i >> 1; // shift the binary form 1 digit to the right
			nextStateTable[i][1] = (i >> 1) + (int)Math.pow(2, k - 2); // shift 1 digit to the right and add 2^(k-2)
		}
		
		return nextStateTable;
	}
	
	
	// output table, row x column = 2^(k - 1) x 2:
	// output from the two channels at each state with input 0 or 1
	// k is the constraint length, k - 1 is number of registers; c1 and c2 are channel 1 and 2's XOR pattern
	public static int[][] getOutputTable(int k, int c1, int c2) {
		int numOfRows = (int) Math.pow(2, k - 1);
		int[][] outputTable = new int[numOfRows][2];
		String c1Pat = getBinary(c1); // XOR pattern in channel 1
		String c2Pat = getBinary(c2); // XOR pattern in channel 2
		
		// reverse pattern bits since it's opposite to the normal notation
		for(int i = c1Pat.length(); i < k; i++)
			c1Pat = '0' + c1Pat;
		for(int i = c2Pat.length(); i < k; i++)
			c2Pat = '0' + c2Pat;
		c1Pat = new StringBuilder(c1Pat).reverse().toString();
		c2Pat = new StringBuilder(c2Pat).reverse().toString();
		
		for(int i = 0; i < numOfRows; i++) {
			// calculate output for input 0
			outputTable[i][0] = getChannelOutput(0, i, k, c1Pat) * 2 + getChannelOutput(0, i, k, c2Pat);
			// calculate output for input 1
			outputTable[i][1] = getChannelOutput(1, i, k, c1Pat) * 2 + getChannelOutput(1, i, k, c2Pat);
		}
		
		return outputTable;
	}
	
	// get XOR output from a channel with specific input, state, constraint length and pattern
	public static int getChannelOutput(int input, int state, int len, String pattern) {
		String stateString = getBinary(state);
		// get the whole string including input and state
		for(int i = stateString.length(); i < len - 1; i++)
			stateString = '0' + stateString;
		stateString = input + stateString;
		
		int numOfOnes = 0;
		// use pattern to find the number of 1s matching the pattern to do the XOR
		for(int i = 1; i <= pattern.length(); i++) {
			if(pattern.charAt(pattern.length() - i) == '1' && stateString.charAt(stateString.length() - i) == '1')
				numOfOnes++;
		}
		if(numOfOnes % 2 == 0) return 0; // there are even number of 1s in the channel for XOR
		return 1;
	}
	
	// get the binary string of integer
	public static String getBinary(int n) {
		String str = "";
		while(n != 0) {
			str = n % 2 + str;
			n = n / 2;
		}
		return str;
	}
	
	// input table, row x column = 2^(k - 1) x 2^(k - 1):
	// find input to convert current state to next state
	public static int[][] getInputTable(int k) {
		int numOfRows = (int) Math.pow(2, k - 1);
		int[][] inputTable = new int[numOfRows][numOfRows];
		
		// each state can only reach two different states with input 0 or 1
		// input 0: shift 1 digit to the right and add 0 to the leftmost register
		// input 1: shift 1 digit to the right and add 1 to the leftmost register
		for(int i = 0; i < numOfRows; i++)
			for(int j = 0; j < numOfRows; j++) {
				if(i / 2 == j) inputTable[i][j] = 0;
				else if(i / 2 + (int) Math.pow(2, k - 2) == j) inputTable[i][j] = 1;
				else inputTable[i][j] = -1;
			}
		return inputTable;
	}

}

