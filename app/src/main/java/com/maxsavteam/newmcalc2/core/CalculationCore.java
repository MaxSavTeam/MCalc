package com.maxsavteam.newmcalc2.core;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.maxsavteam.newmcalc2.R;
import com.maxsavteam.newmcalc2.types.Fraction;
import com.maxsavteam.newmcalc2.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import ch.obermuhlner.math.big.BigDecimalMath;

/**
 * @author Max Savitsky
 */
public final class CalculationCore{
	private CoreLinkBridge coreLinkBridge;

	private Resources mResources;
	private String invalidArgument, valueIsTooBig, divisionByZero;
	private Context c;
	private final String TAG = "Core";
	private String bracketFloorOpen, bracketFloorClose,
			bracketCeilOpen, bracketCeilClose;
	private String mExample, mType;

	/**
	 * used for actions
	 * */
	private final Stack<String> s0 = new Stack<>();

	/**
	 * used for numbers
	 * */
	private final Stack<BigDecimal> s1 = new Stack<>();
	private boolean mWasError = false;

	public CalculationCore(Context context) {
		initialize( context );
	}

	private void initialize(Context context){
		this.mResources = context.getApplicationContext().getResources();
		this.c = context;
		invalidArgument = mResources.getString( R.string.invalid_argument );
		valueIsTooBig = mResources.getString( R.string.value_is_too_big );
		divisionByZero = mResources.getString( R.string.division_by_zero );

		bracketFloorOpen = mResources.getString( R.string.bracket_floor_open );
		bracketFloorClose = mResources.getString( R.string.bracket_floor_close );
		bracketCeilOpen = mResources.getString( R.string.bracket_ceil_open );
		bracketCeilClose = mResources.getString( R.string.bracket_ceil_close );
	}

	public void prepare(String example, @Nullable String type){
		this.mExample = example;
		this.mType = type;
	}

	private boolean isOpenBracket(String str){
		return str.equals( "(" ) ||
				str.equals( bracketCeilOpen ) ||
				str.equals( bracketFloorOpen );
	}

	private boolean isOpenBracket(char c){
		String str = Character.toString( c );
		return isOpenBracket( str );
	}

	private boolean isCloseBracket(String str){
		return str.equals( ")" ) ||
				str.equals( bracketFloorClose ) ||
				str.equals( bracketCeilClose );
	}

	private boolean isCloseBracket(char c){
		return isCloseBracket( String.valueOf( c ) );
	}

	private String getTypeOfBracket(String bracket){
		if(bracket.equals( "(" ) || bracket.equals( ")" ))
			return "simple";
		if(bracket.equals( bracketFloorClose ) || bracket.equals( bracketFloorOpen ))
			return "floor";
		if(bracket.equals( bracketCeilOpen ) || bracket.equals( bracketCeilClose ))
			return "ceil";
		return "undefined";
	}

	private String getTypeOfBracket(char c){
		return getTypeOfBracket( Character.toString( c ) );
	}

	public final void setInterface(CoreLinkBridge clb) {
		this.coreLinkBridge = clb;
	}
	
	public interface CoreLinkBridge{
		void onSuccess(CalculationResult calculationResult);
		void onError(CalculationError calculationError);
	}

	private void onSuccess(CalculationResult calculationResult) {
		coreLinkBridge.onSuccess( calculationResult );
		//currentThread().interrupt();
	}

	private void onError(CalculationError calculationError) {
		coreLinkBridge.onError( calculationError );
		//currentThread().interrupt();
	}

	private BigDecimal rootWithBase(BigDecimal a, BigDecimal n){
		Log.v( TAG, "rootBase called with a=" + a.toPlainString() + " n=" + n.toPlainString() );

		BigDecimal log = BigDecimalMath.log(a, new MathContext(20));
		BigDecimal dLog = log.divide(n, 20, RoundingMode.HALF_EVEN);
		return BigDecimalMath.exp(dLog, new MathContext(8));
	}

	private final BigDecimal MAX_FACTORIAL_VALUE = new BigDecimal( "1000" );
	private final BigDecimal MAX_POW = new BigDecimal( "10000000000" );

	/**
	 * Performs all necessary checks and changes, and if everything is in order, starts the core (calculation)
	 *
	 * @param exampleArg expression to be calculated
	 * @param type type of calculation. Can be null
	 * @throws NullPointerException throws, when interface hasn't been set
	 *
	 * @see CalculationResult
	 */
	public void prepareAndRun(@NotNull final String exampleArg, @Nullable String type) throws NullPointerException{
		if(coreLinkBridge == null)
			throw new NullPointerException("Calculation Core: Interface wasn't set");
		String example = String.copyValueOf( exampleArg.toCharArray() );
		int len = example.length();
		char last;
		if(len == 0)
			return;
		else
			last = example.charAt(len - 1);

		if(last == '(')
			return;

		if(example.contains( bracketFloorOpen ) || example.contains( bracketCeilOpen ) || example.contains( "(" )) {
			Stack<Character> bracketsStack = new Stack<>();
			for (int i = 0; i < example.length(); i++) {
				char cur = example.charAt(i);
				if (isOpenBracket( cur ) )
					bracketsStack.push( cur );
				else if (isCloseBracket( cur )) {
					try {
						bracketsStack.pop();
					}catch (EmptyStackException e){
						e.printStackTrace();
						mWasError = true;
						onError(new CalculationError().setStatus("Core"));
						return;
					}
				}
			}
			if (!bracketsStack.isEmpty()) {
				while(!bracketsStack.isEmpty()){
					String br = "";
					String typeOf = getTypeOfBracket( bracketsStack.peek() );
					switch ( typeOf ) {
						case "simple":
							br = ")";
							break;
						case "floor":
							br = bracketFloorClose;
							break;
						case "ceil":
							br = bracketCeilClose;
							break;
					}
					Log.v( TAG, "appending brackets; bracketsStack size=" + bracketsStack.size() + "; bracket type=" + typeOf + " bracket=" + br );
					example = String.format( "%s%s", example, br );
					bracketsStack.pop();
				}
				Log.v( TAG, "example after appending ex=" + example );
			}
		}
		if(example.contains(" ")){
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < example.length(); i++) {
				if (example.charAt(i) != ' ')
					sb.append(example.charAt(i));
			}
			example = sb.toString();
		}
		if(Utils.isNumber(example)){
			onError(new CalculationError().setStatus("Core").setErrorMessage("String is number").setPossibleResult(new BigDecimal(example)));
			return;
		}
		if(example.contains( mResources.getString(R.string.multiply)) || example.contains( mResources.getString(R.string.div))
				|| example.contains( mResources.getString(R.string.pi)) || example.contains( mResources.getString(R.string.fi))
				|| example.contains( mResources.getString(R.string.sqrt))){
			char[] mas = example.toCharArray();
			String p;

			for(int i = 0; i < example.length(); i++){
				p = Character.toString(mas[i]);
				if(p.equals( mResources.getString(R.string.div))){
					mas[i] = '/';
				}else if(p.equals( mResources.getString(R.string.multiply))){
					mas[i] = '*';
				}else if(p.equals( mResources.getString(R.string.pi))){
					mas[i] = 'P';
				}else if(p.equals( mResources.getString(R.string.fi))){
					mas[i] = 'F';
				} else if ( p.equals( mResources.getString( R.string.sqrt ) ) ) {
					mas[ i ] = 'R';
				}
			}
			example = new String( mas );
		}

		calculate( example, type );
	}

	private static class CoreSubProcess implements CoreLinkBridge {
		private BigDecimal res;
		private Context mContext;

		private String TAG = "CoreSubProcess";

		private CalculationError getError() {
			return error;
		}

		private boolean isWasError() {
			return mWasError;
		}

		private CalculationError error;
		private boolean mWasError = false;

		public BigDecimal getRes() {
			return res;
		}

		@Override
		public void onSuccess(CalculationResult calculationResult) {
			res = calculationResult.getResult();
		}

		@Override
		public void onError(CalculationError error) {
			mWasError = true;
			this.error = error;
			if(error.getStatus().equals("Core")) {
				if (error.getErrorMessage().contains("String is number")) {
					res = error.getPossibleResult();
				}
			}
		}

		private void run(String ex) {
			CalculationCore calculationCore = new CalculationCore(mContext);
			calculationCore.setInterface(this);

			Log.v( TAG, "run with ex=" + ex );
			calculationCore.prepareAndRun(ex, "isolated");
		}

		CoreSubProcess(Context context){
			this.mContext = context;
			Log.v( TAG, "constructor" );
		}
	}

	private static class MovedExample{
		String subExample;
		int newPos;

		MovedExample(String subExample, int newPos){
			this.subExample = subExample;
			this.newPos = newPos;
		}
	}

	private MovedExample getSubExampleFromBrackets(String example, int pos){
		int i = pos + 1;
		String subExample = "";
		int bracketsLvl = 0;
		while ( bracketsLvl != 0 || !isCloseBracket( example.charAt( i ) ) ) {
			char now = example.charAt( i );
			if ( isOpenBracket( now ) )
				bracketsLvl++;
			else if ( isCloseBracket( now ) )
				bracketsLvl--;

			subExample = String.format( "%s%c", subExample, now );
			i++;
		}
		return new MovedExample( subExample, i );
	}

	private void calculate(String example, String type) {
		s0.clear();
		s1.clear();
		mWasError = false;
		String x;
		String s;
		int len = example.length();

		for (int i = 0; i < len; i++) {

			if(mWasError || Thread.currentThread().isInterrupted()) {
				Log.v( TAG, "Main loop destroyer was called; mWasError=" + mWasError + "; Thread.currentThread().isInterrupted()=" + Thread.currentThread().isInterrupted() );
				return;
			}

			try {
				s = Character.toString( example.charAt( i ) );

				if(isOpenBracket( s )){
					MovedExample movedExample = getSubExampleFromBrackets( example, i );
					String subExample = movedExample.subExample;
					i = movedExample.newPos;
					CoreSubProcess coreSubProcess = new CoreSubProcess( c );
					coreSubProcess.run( subExample );
					BigDecimal result;
					if( coreSubProcess.isWasError()){
						if( coreSubProcess.getError().getErrorMessage().contains( "String is number" )){
							result = coreSubProcess.getRes();
						}else{
							mWasError = true;
							onError( coreSubProcess.getError() );
							return;
						}
					}else{
						result = coreSubProcess.getRes();
					}

					String bracketType = getTypeOfBracket( s );
					switch ( bracketType ){
						case "simple":
							s1.push( result );
							break;
						case "ceil":
							s1.push( result.setScale( 0, RoundingMode.CEILING ) );
							break;
						case "floor":
							s1.push( result.setScale( 0, RoundingMode.FLOOR ) );
							break;
						default:
							mWasError = true;
							onError( new CalculationError().setStatus( "Core" ).setErrorMessage( "type of bracket is undefined" ) );
							return;
					}

					continue;
				}

				if ( s.equals( "s" ) || s.equals( "t" ) || s.equals( "l" ) || s.equals( "c" ) || s.equals( "a" ) ) {
					if ( i != 0 ) {
						if ( isCloseBracket( example.charAt( i - 1 ) ) ) {
							s0.push("*");
						}
					}
					//if(i + 4 <= example.length()){
					String let = "";
					while (i < example.length() && !isOpenBracket( example.charAt( i ) )) {
						let = String.format("%s%c", let, example.charAt(i));
						i++;
					}
					i--;
					s0.push( let );
					continue;
				} else if (s.equals("P")) {
					BigDecimal f = new BigDecimal(Math.PI);
					s1.push(f);
					if (i != 0 && Utils.isDigit(example.charAt(i - 1))) {
						in_s0('*');
					}
					char next = '\0';
					if (i != example.length() - 1)
						next = example.charAt(i + 1);
					if (i != example.length() - 1 && (Utils.isDigit(example.charAt(i + 1)) || next == 'F' || next == 'P' || next == 'e')) {
						in_s0('*');
					}
					//s1.push(f);
					continue;
				} else if (s.equals("F")) {
					BigDecimal f = new BigDecimal(1.618);
					s1.push(f);
					if (i != 0 && Utils.isDigit(example.charAt(i - 1))) {
						in_s0('*');
					}
					if (i != example.length() - 1) {
						char next = example.charAt(i + 1);
						if (Utils.isDigit(example.charAt(i + 1)) || next == 'F' || next == 'P' || next == 'e') {
							in_s0('*');
						}
					}
					continue;
				} else if (s.equals("!")) {
					try {
						if (i != len - 1 && example.charAt(i + 1) == '!') {
							BigDecimal y = s1.peek(), ans = BigDecimal.ONE;
							boolean isNumberBigger = y.compareTo(MAX_FACTORIAL_VALUE) > 0;
							if (y.signum() < 0 || isNumberBigger) {
								mWasError = true;
								if ( isNumberBigger ) {
									onError( new CalculationError().setErrorMessage( "Invalid argument: factorial value is too much" ).setShortError( valueIsTooBig ) ); // I do not know how to name this error
								}
								break;
							}
							for (; y.compareTo(BigDecimal.valueOf(0)) > 0; y = y.subtract(BigDecimal.valueOf(2))) {
								ans = ans.multiply(y);
							}
							i++;
							s1.pop();
							s1.push(ans);
							continue;
						} else {
							BigDecimal y = s1.peek();
							if (y.signum() < 0) {
								mWasError = true;
								onError( new CalculationError().setErrorMessage( "Error: Unable to find negative factorial." ).setShortError( invalidArgument ) );
								break;
							} else {
								if (y.compareTo(MAX_FACTORIAL_VALUE) > 0) {
									mWasError = true;
									onError( new CalculationError().setErrorMessage( "For some reason, we cannot calculate the factorial of this number " +
											"(because it is too large and may not have enough device resources when executed)" ).setShortError( valueIsTooBig ) );
									break;
								} else {
									s1.pop();
									s1.push(Utils.fact(y));
								}
							}
							if (i != len - 1) {
								char next = example.charAt(i + 1);
								if (Utils.isDigit(next) || next == 'P' || next == 'F' || next == 'e')
									in_s0('*');
							}
							continue;
						}
					} catch (Exception e) {
						e.printStackTrace();
						mWasError = true;
						onError( new CalculationError().setErrorMessage( e.toString() ).setMessage( e.getMessage() ).setShortError( valueIsTooBig ) );
						break;
					}
				} else if (s.equals("%")) {
					if (s0.empty() || (!s0.empty() && !Utils.isBasicAction(s0.peek()))) {
						BigDecimal y = s1.peek();
						s1.pop();
						y = y.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN);
						y = new BigDecimal(Utils.deleteZeros(y.toPlainString()));
						s1.push(y);
						if (i != len - 1) {
							char next = example.charAt(i + 1);
							if (Utils.isDigit(next) || next == 'P' || next == 'F' || next == 'e')
								in_s0('*');
						}
						continue;
					} else if (!s0.empty() && Utils.isBasicAction(s0.peek())) {
						try {

							i++;
							String x1 = "";
							int brackets = 0;
							while (i < example.length()) {
								if(brackets == 0 && (example.charAt(i) == '-' || example.charAt(i) == '+')){
									break;
								}
								if(brackets == 0 && isCloseBracket( example.charAt( i ) ))
									break;

								if(isOpenBracket( example.charAt(i)) )
									brackets++;
								else if(isCloseBracket( example.charAt(i) ))
									brackets--;

								x1 = String.format("%s%c", x1, example.charAt(i));
								i++;
							}
							x1 = s1.peek().toPlainString() + x1;
							s1.pop();
							CoreSubProcess coreSubProcess = new CoreSubProcess(c);
							coreSubProcess.run(x1);
							if ( coreSubProcess.isWasError() && !coreSubProcess.getError().getErrorMessage().contains("String is number")) {
								mWasError = true;
								onError( coreSubProcess.getError() );
								return;
							} else {
								BigDecimal top;
								top = coreSubProcess.getRes();
								top = top.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN);
								top = new BigDecimal(Utils.deleteZeros(top.toPlainString()));
								//s1.push(top);
								BigDecimal finalResult = s1.peek(), percent;
								s1.pop();
								percent = finalResult.multiply(top);
								if (!s0.empty()) {
									String action = s0.peek();
									s0.pop();

									if (Utils.isBasicAction(action)) {
										switch (action) {
											case "+":
												finalResult = finalResult.add(percent);
												break;
											case "-":
												finalResult = finalResult.subtract(percent);
												break;
											case "*":
												finalResult = finalResult.multiply(percent);
												break;
											case "/":
												finalResult = finalResult.divide(percent, 10, RoundingMode.HALF_EVEN);
												finalResult = new BigDecimal(Utils.deleteZeros(finalResult.toPlainString()));
												break;
										}
										finalResult = new BigDecimal(Utils.deleteZeros(finalResult.toPlainString()));
										s1.push(finalResult);
									}
								}

							}
							i--;
							continue;
						} catch (Exception e) {
							e.printStackTrace();
							mWasError = true;
							onError( new CalculationError().setErrorMessage( e.toString() ).setMessage( e.getMessage() ).setShortError( valueIsTooBig ) );
							break;
						}
					}
				} else if (s.equals("e")) {
					BigDecimal f = new BigDecimal(Math.E);
					s1.push(f);
					if (i != 0 && Utils.isDigit(example.charAt(i - 1))) {
						in_s0('*');
					}
					if (i != example.length() - 1 && Utils.isDigit(example.charAt(i + 1))) {
						in_s0('*');
					}
					continue;
				} else if (s.equals("R")) {
					if (i == len - 1) {
						mWasError = true;
						break;
					} else {
						if (example.charAt(i + 1) == '(') {
							in_s0('R');
							continue;
						} else {
							mWasError = true;
							onError( new CalculationError().setErrorMessage( "Invalid statement for square root" ).setShortError( invalidArgument ) );
							break;
						}
					}
				} else if (s.equals("A")) {
					i += 2;
					String n = "";
					int actions = 0;
					while (example.charAt(i) != ')') {
						if (example.charAt(i) == '+') {
							actions++;
							s1.push(new BigDecimal(n));
							n = "";
						} else {
							n = String.format( "%s%c", n, example.charAt( i ) );
						}
						i++;
					}
					s1.push( new BigDecimal( n ) );
					BigDecimal sum = BigDecimal.ZERO;
					for (int j = 0; j <= actions; j++) {
						sum = sum.add( s1.peek() );
						s1.pop();
					}
					sum = sum.divide( BigDecimal.valueOf( actions + 1 ), 4, RoundingMode.HALF_EVEN );
					String answer = sum.toPlainString();
					s1.push( new BigDecimal( Utils.deleteZeros( answer ) ) );
					continue;
				} else if (s.equals("G")) {
					i += 2;
					String n = "";
					int actions = 0;
					while (example.charAt(i) != ')') {
						if (example.charAt(i) == '*') {
							actions++;
							s1.push(new BigDecimal(n));
							n = "";
						} else {
							n = String.format( "%s%c", n, example.charAt( i ) );
						}
						i++;
					}
					s1.push( new BigDecimal( n ) );
					BigDecimal pr = BigDecimal.ONE;
					for (int j = 0; j <= actions; j++) {
						pr = pr.multiply( s1.peek() );
						s1.pop();
					}
					//sum = BigDecimal.valueOf(Math.sqrt(sum.doubleValue())); // BigDecimal has method BigDecimal.abs(), but it is available in Java 9 and high, Android uses Java 8
					//pr = BigDecimalMath.sqrt( pr, new MathContext( 10 ) );
					pr = rootWithBase( pr, BigDecimal.valueOf( actions + 1 ) );
					String answer = pr.toPlainString();
					s1.push( new BigDecimal( Utils.deleteZeros( answer ) ) );
					continue;
				}
				if (Utils.isDigit(example.charAt(i))) {
					x = "";
					while ((i < example.length()) && ((example.charAt(i) == '.') || Utils.isDigit(example.charAt(i)) || (example.charAt(i) == '-' && example.charAt(i - 1) == 'E'))) {
						x = String.format("%s%c", x, example.charAt(i));
						i++;
					}
					s1.push(new BigDecimal(x));
					i--;
				} else {
					if (example.charAt(i) != ')') {
						if (example.charAt(i) == '^') {
							if (i != example.length() - 1 && example.charAt(i + 1) == '(') {
								in_s0('^');
								//i++;
								//s0.push("(");
								continue;
							}
						}
						if ((i == 0 && example.charAt(i) == '-') || (example.charAt(i) == '-' && example.charAt(i - 1) == '(')) {
							x = "";
							i++;
							while ((i < example.length()) && ((example.charAt(i) == '.') || Utils.isDigit(example.charAt(i)) || example.charAt(i) == 'E' || (example.charAt(i) == '-' && example.charAt(i - 1) == 'E'))) {
								x = String.format("%s%c", x, example.charAt(i));
								i++;
							}
							i--;
							s1.push(new BigDecimal(x).multiply(BigDecimal.valueOf(-1)));
							continue;
						}
						if (i != 0 && example.charAt(i) == '(' && (Utils.isDigit(example.charAt(i - 1)) || example.charAt(i - 1) == ')')) {
							in_s0('*');
						}

						in_s0(example.charAt(i));
					} /*else {
						while (!s0.empty() && !s0.peek().equals("(")) {
							mult( s0.peek() );
							if ( mWasError ) {
								break;
							}
							s0.pop();
						}
						if (!s0.empty() && s0.peek().equals("(")) {
							s0.pop();
						}
						if (i != example.length() - 1) {
							if (Utils.isDigit(example.charAt(i + 1))) {
								in_s0('*');
							}
						}
					}*/
				}
			}catch (Exception e){
				mWasError = true;
				onError( new CalculationError().setStatus( "Core" ).setShortError( "Smth went wrong" ) );
				break;
			}
		}
		try {
			while ( !mWasError && !s0.isEmpty() && s1.size() >= 2 ) {
				mult( s0.peek() );
				if ( mWasError ) {
					break;
				}
				s0.pop();
			}
			if ( !mWasError && !s0.isEmpty() && s1.size() == 1 ) {
				if ( s0.peek().equals( "R" ) ) {
					mult( s0.peek() );
					s0.pop();
				}
				if ( !s0.isEmpty() && ( s0.peek().length() == 3 || s0.peek().equals( "ln" )  ) ) {
					mult( s0.peek() );
					s0.pop();
				}
			}
		} catch (Exception e) {
			mWasError = true;
			onError( new CalculationError().setStatus( "Core" ) );
		}
		if ( !mWasError ) {
			onSuccess( new CalculationResult().setResult( s1.peek() ).setType( type ) );
		}
	}

	private void mult(String x) throws Exception {
		try {
			if (x.length() == 3 || x.equals("ln") || x.equals("R")) {
				double d = s1.peek().doubleValue();
				BigDecimal operand = s1.peek();
				BigDecimal ans = BigDecimal.ONE;
				if (x.equals("log") && d <= 0) {
					mWasError = true;
					onError( new CalculationError().setErrorMessage( "You cannot find the logarithm of a zero or a negative number." ).setShortError( invalidArgument ) );
					return;
				}
				s1.pop();
				//d = Math.toRadians(d);
				switch (x) {
					case "cos": {
						//ans = BigDecimal.valueOf(Math.cos(Math.toRadians(d)));
						ans = BigDecimalMath.cos(Utils.toRadians(operand), new MathContext(9));
						break;
					}
					case "sin": {
						ans = BigDecimalMath.sin(Utils.toRadians(operand), new MathContext(9));
						break;
					}
					case "tan": {
						ans = BigDecimalMath.tan(Utils.toRadians(operand), new MathContext(9));
						break;
					}
					case "log": {
						if (operand.signum() <= 0) {
							mWasError = true;
							onError( new CalculationError().setErrorMessage( "Illegal argument: unable to find log of " + ( d == 0 ? "zero." : "negative number." ) ).setShortError( invalidArgument ) );
							return;
						}
						//ans = BigDecimal.valueOf(Math.log10(d));
						ans = BigDecimalMath.log10(operand, new MathContext(9));
						break;
					}
					case "abs":
						if(operand.signum() < 0){
							ans = operand.multiply( BigDecimal.valueOf( -1 ) );
						}else{
							ans = operand;
						}
						break;
					case "ln": {
						if (operand.signum() <= 0) {
							mWasError = true;
							onError( new CalculationError().setErrorMessage( "Illegal argument: unable to find ln of " + ( d == 0 ? "zero." : "negative number." ) ).setShortError( invalidArgument ) );
							return;
						}
						//ans = BigDecimal.valueOf(Math.log(d));
						ans = BigDecimalMath.log(operand, new MathContext(9));
						break;
					}
					case "R":
						if (operand.signum() < 0) {
							mWasError = true;
							onError( new CalculationError().setErrorMessage( "Invalid argument: the root expression cannot be negative." ).setShortError( invalidArgument ) );
							return;
						}
						ans = BigDecimalMath.sqrt(operand, new MathContext(9));
						break;
				}
				ans = ans.divide(BigDecimal.valueOf(1.0), 9, RoundingMode.HALF_EVEN);
				String answer = ans.toPlainString();
				s1.push(new BigDecimal(Utils.deleteZeros(answer)));
				return;
			}
			BigDecimal b = s1.peek();
			s1.pop();
			BigDecimal a = s1.peek();
			BigDecimal ans = s1.peek();
			s1.pop();
			try {
				switch (x) {
					case "+":
						ans = a.add(b);
						break;
					case "-":
						ans = a.subtract(b);
						break;
					case "*":
						ans = a.multiply(b);
						break;
					case "/":
						if (b.signum() == 0) {
							mWasError = true;
							onError( new CalculationError().setErrorMessage( "Division by zero." ).setShortError( divisionByZero ) );
							return;
						}
						ans = a.divide(b, 9, RoundingMode.HALF_EVEN);
						break;

					case "^":
						if(b.compareTo(MAX_POW) > 0){
							mWasError = true;
							onError( new CalculationError().setShortError( valueIsTooBig ) );
							return;
						} else if(b.signum() == 0 && a.signum() == 0){
							mWasError = true;
							onError( new CalculationError().setShortError( "Raising zero to zero degree." ) );
							return;
						}
						String power = b.toPlainString();
						power = Utils.deleteZeros(power);
						if(power.contains(".")){
							Fraction fraction = new Fraction(power);
							//a = BigDecimalMath.pow(a, fraction.getNumerator(), new MathContext(10));
							a = Utils.pow( a, fraction.getNumerator() );
							ans = rootWithBase( a, fraction.getDenominator() );
						}else{
							ans = Utils.pow( a, b );
						}
						Log.v( TAG, "pow answer returned ans=" + ans );
						break;
				}
				String answer = ans.toPlainString();
				ans = new BigDecimal(Utils.deleteZeros(answer));
				s1.push(ans);
			} catch (ArithmeticException e) {
				String str = e.toString();
				if (str.contains("Non-terminating decimal expansion; no exact representable decimal result")) {
					ans = a.divide(b, 4, RoundingMode.HALF_EVEN);
					ans = new BigDecimal(Utils.deleteZeros(ans.toPlainString()));
					s1.push(ans);
				}else if(str.contains("Infinity or Nan")){
					mWasError = true;
					onError( new CalculationError().setErrorMessage( e.toString() ).setShortError( valueIsTooBig ) );
				} else {
					mWasError = true;
					onError( new CalculationError().setErrorMessage( e.toString() ).setMessage( e.getMessage() ) );
				}
			}
		}catch(EmptyStackException e){
			mWasError = true;
			onError( new CalculationError().setStatus( "Core" ).setErrorMessage( e.toString() ) );
		} catch (Exception e) {
			mWasError = true;
			onError( new CalculationError().setErrorMessage( e.toString() ).setMessage( e.getMessage() ) );
			throw new Exception( e.getMessage() );
		}
	}

	private void in_s0(char x) throws Exception{
		Map<String, Integer> priority = new HashMap<>();
		priority.put("(", 0);
		priority.put("-", 1);
		priority.put("+", 1);
		priority.put("/", 2);
		priority.put("*", 2);
		priority.put("^", 3);
		priority.put("R", 3);

		priority.put("cos", 3);
		priority.put("tan", 3);
		priority.put("sin", 3);
		priority.put("ln", 3);
		priority.put("log", 3);

		priority.put("abs", 3);

		Log.v( TAG, "in_s0 called with x=" + x + "; s0.size()=" + s0.size() );

		if(s0.empty()) {
			s0.push(Character.toString(x));
			return;
		}

		Integer priorityOfX = priority.get(Character.toString(x));
		Integer priorityOfTopAction = priority.get(s0.peek());

		if(priorityOfX == null || priorityOfTopAction == null) {
			Log.v(TAG, "in_s0: priority.get returned null on " + s0.peek());

			NullPointerException nullPointerException = new NullPointerException( "Method: in_s0; priorityOfX equals null or priorityOfTopAction equals null" );
			nullPointerException.printStackTrace();
			throw nullPointerException;
		}

		if(x == '(') {
			s0.push(Character.toString(x));
			return;
		}

		if(s0.peek().equals("(")){
			s0.push(Character.toString(x));
			return;
		}
		if (priorityOfX < priorityOfTopAction || priorityOfX.equals(priority.get(s0.peek()))) {
			mult(s0.peek());
			s0.pop();
			in_s0(x);
			return;
		}
		if(priorityOfX > priorityOfTopAction) {
			s0.push(Character.toString(x));
		}
	}

	public static final class CalculationResult {
		private String mType = null;
		private BigDecimal mResult = null;

		@javax.annotation.Nullable
		public final String getType() {
			return mType;
		}

		public final CalculationResult setType(String type) {
			mType = type;
			return this;
		}

		@javax.annotation.Nullable
		public final BigDecimal getResult() {
			return mResult;
		}

		public final CalculationResult setResult(BigDecimal result) {
			mResult = result;
			return this;
		}
	}
}

