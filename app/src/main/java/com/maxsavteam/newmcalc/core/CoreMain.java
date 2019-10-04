package com.maxsavteam.newmcalc.core;

import android.widget.Toast;

import com.maxsavteam.newmcalc.R;
import com.maxsavteam.newmcalc.utils.Utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public final class CoreMain {
	private CoreLinkBridge coreLinkBridge;
	private boolean was_error = false;
	
	public void setInterface(CoreLinkBridge clb){
		this.coreLinkBridge = clb;
	}
	
	public interface CoreLinkBridge{
		void onSuccess(BigDecimal result, String type);
		void onError(String error);
	}

	private static Stack<String> s0 = new Stack<>();
	private static Stack<BigDecimal> s1 = new Stack<>();
	
	public void prepare(String example, String type){
		calc(example, type);
	}

	private void calc(String stri, String type){
		s0.clear();
		s1.clear();
		char[] str = new char[stri.length()];
		stri.getChars(0, stri.length(), str, 0);
		String x;
		String s;
		int len = stri.length();

		for(int i = 0; i < len; i++){
			s = Character.toString(str[i]);
			if(s.equals("s") || s.equals("t") || s.equals("l") || s.equals("c")){
				if(i != 0){
					if(stri.charAt(i-1) == ')'){
						s0.push("*");
					}
				}
				//if(i + 4 <= stri.length()){
				String let = "";
				while(i < stri.length() && Utils.islet(stri.charAt(i))){
					let += Character.toString(stri.charAt(i));
					i++;
				}
				switch (let) {
					case "sin":
						s0.push("sin");
						s0.push("(");
						break;
					case "cos":
						s0.push("cos");
						s0.push("(");
						break;
					case "tan":
						s0.push("tag");
						s0.push("(");
						break;
					case "log":
						s0.push("log");
						s0.push("(");
						break;
					case "ln":
						s0.push("ln");
						s0.push("(");
						break;
				}
				continue;
                /*}else{
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalidstateforsin), Toast.LENGTH_LONG).show();
                    was_error = true;
                    break;
                    /*if(i != 0 && !Utils.isdigit(stri.charAt(i-1))){
                        if(stri.charAt(i-1) == '(' || stri.charAt(i-1) == '.'){
                            break;
                        }else{
                            s0.pop();
                        }
                    }
                }*/
			}
			if(s.equals("P")){
				BigDecimal f = new BigDecimal(Math.PI);
				s1.push(f);
				if(i != 0 && Utils.isdigit(stri.charAt(i-1))){
					in_s0('*');
				}
				char next = '\0';
				if(i != stri.length() - 1)
					next = stri.charAt(i+1);
				if(i != stri.length()-1 && (Utils.isdigit(stri.charAt(i+1))  || next == 'F' || next == 'P' || next == 'e')){
					in_s0('*');
				}
				//s1.push(f);
				continue;
			}else if(s.equals("F")){
				BigDecimal f = new BigDecimal(1.618);
				s1.push(f);
				if(i != 0 && Utils.isdigit(stri.charAt(i-1))){
					in_s0('*');
				}
				char next = '\0';
				if(i != stri.length() - 1)
					next = stri.charAt(i+1);
				if(i != stri.length()-1 && (Utils.isdigit(stri.charAt(i+1))  || next == 'F' || next == 'P' || next == 'e')){
					in_s0('*');
				}
				continue;
			}else if(s.equals("!")){
				if(i != len - 1 && stri.charAt(i + 1) == '!'){
					BigDecimal y = s1.peek(), ans = BigDecimal.ONE;
					if (y.signum() < 0 || y.compareTo(BigDecimal.valueOf(500)) > 0){
						was_error = true;
						coreLinkBridge.onError(""); // I do not know how to name this error
						break;
					}
					for(; y.compareTo(BigDecimal.valueOf(0)) > 0; y = y.subtract( BigDecimal.valueOf(2) ) ){
						ans = ans.multiply(y);
					}
					i++;
					s1.pop();
					s1.push(ans);
					continue;
				}else {
					BigDecimal y = s1.peek();
					if (y.signum() == -1) {
						was_error = true;
						coreLinkBridge.onError("Error: Unable to find negative factorial.");
						break;
					} else {
						if (y.compareTo(new BigDecimal(1000)) > 0) {
							was_error = true;
							coreLinkBridge.onError("For some reason, we cannot calculate the factorial of this number " +
									"(because it is too large and may not have enough device resources when executed)");
							break;
						} else {
							s1.pop();
							String fa = y.toString();
							if (fa.contains(".")) {
								int index = fa.lastIndexOf(".");
								fa = fa.substring(0, index);
								y = new BigDecimal(fa);
							}
							s1.push(Utils.fact(y));
						}
					}
					if(i != len - 1) {
						char next = stri.charAt(i + 1);
						if(Utils.isdigit(next) || next == 'P' || next == 'F' || next == 'e')
							in_s0('*');
					}
					continue;
				}
			}else if(s.equals("%")){
				BigDecimal y = s1.peek();
				s1.pop();
				s1.push(y.divide(new BigDecimal(100)));
				if(i != len - 1) {
					char next = stri.charAt(i + 1);
					if(Utils.isdigit(next) || next == 'P' || next == 'F' || next == 'e')
						in_s0('*');
				}
				continue;
			}else if(s.equals("e")){
				BigDecimal f = new BigDecimal(Math.E);
				s1.push(f);
				if(i != 0 && Utils.isdigit(stri.charAt(i-1))){
					in_s0('*');
				}
				if(i != stri.length()-1 && Utils.isdigit(stri.charAt(i+1))){
					in_s0('*');
				}
				continue;
			}else if(s.equals("R")){
				if(i == len-1){
					was_error = true;
					break;
				}else{
					if(stri.charAt(i + 1) == '('){
						in_s0('R');
						continue;
					}else{
						was_error = true;
						coreLinkBridge.onError("Invalid statement for square root");
						break;
					}
				}
			}else if(s.equals("A")){
				i += 2;
				String n = "";
				int actions = 0;
				while(stri.charAt(i) != ')'){
					if(stri.charAt(i) == '+'){
						actions++;
						s1.push(new BigDecimal(n));
						n = "";
					}else{
						n += Character.toString(stri.charAt(i));
					}
					i++;
				}
				s1.push(new BigDecimal(n));
				BigDecimal sum = BigDecimal.ZERO;
				for(int j = 0; j <= actions; j++){
					sum = sum.add(s1.peek());
					s1.pop();
				}
				sum = sum.divide(BigDecimal.valueOf(actions + 1), 2, RoundingMode.HALF_EVEN);
				String answer = sum.toPlainString();
				s1.push(new BigDecimal(Utils.delete_zeros(answer)));
				continue;
			}else if(s.equals("G")){
				i += 2;
				String n = "";
				int actions = 0;
				while(stri.charAt(i) != ')'){
					if(stri.charAt(i) == '*'){
						actions++;
						s1.push(new BigDecimal(n));
						n = "";
					}else{
						n += Character.toString(stri.charAt(i));
					}
					i++;
				}
				s1.push(new BigDecimal(n));
				BigDecimal sum = BigDecimal.ONE;
				for(int j = 0; j <= actions; j++) {
					sum = sum.multiply(s1.peek());
					s1.pop();
				}
				sum = BigDecimal.valueOf(Math.sqrt(sum.doubleValue())); // BigDecimal has method BigDecimal.abs(), but it is available in Java 9 and high, Android uses Java 8
				String answer = sum.toPlainString();
				s1.push(new BigDecimal(Utils.delete_zeros(answer)));
				continue;
			}
			if(Utils.isdigit(str[i])){
				x = "";
				while((i < stri.length()) && ((stri.charAt(i) == '.') || Utils.isdigit(str[i]) || (stri.charAt(i) == '-' && stri.charAt(i-1) == 'E'))){
					s = Character.toString(str[i]);
					x += s;
					i++;
				}
				BigDecimal temp = new BigDecimal(x);
				if(temp.divide(BigDecimal.ONE, 2, RoundingMode.HALF_DOWN).equals(BigDecimal.valueOf(3.14))){
					x = Double.toString(Math.PI);
				}else if(temp.divide(BigDecimal.ONE, 3, RoundingMode.FLOOR).equals(BigDecimal.valueOf(1.618))){
					x = "1.618";
				}
				s1.push(new BigDecimal(x));
				if(i < stri.length() && str[i] == 'E'){
					in_s0('^');
					i++;
					BigDecimal t = BigDecimal.ONE;
					if(str[i] == '+'){
						t = BigDecimal.ONE;
					}else if(str[i] == '-'){
						t = BigDecimal.valueOf(-1.0);
					}
					x = "";
					while(i < stri.length() && (stri.charAt(i) == '.' || Utils.isdigit(stri.charAt(i)))){
						x += Character.toString(stri.charAt(i));
						i++;
					}
					s1.push(new BigDecimal(x).multiply(t));
				}
				i--;
			}else{
				if(str[i] != ')'){
					if(str[i] == '^'){
						if(i != stri.length()-1 && str[i + 1] == '('){
							i++;
							in_s0('^');
							s0.push("(");
							continue;
						}else if(i != stri.length()-1 && str[i + 1] != '('){
							//i++;
							in_s0('^');
							continue;
						}
					}
					if((i == 0 && str[i] == '-') || (str[i] == '-' && stri.charAt(i-1) == '(')){
						x = "";
						i++;
						while((i < stri.length()) && ((stri.charAt(i) == '.') || Utils.isdigit(str[i]) || stri.charAt(i) == 'E' || (stri.charAt(i) == '-' && stri.charAt(i-1) == 'E'))){
							s = Character.toString(str[i]);
							x += s;
							i++;
						}
						i--;
						s1.push(new BigDecimal(x).multiply(BigDecimal.valueOf(-1)));
						continue;
					}
					if(i != 0 && str[i] == '(' && (Utils.isdigit(str[i-1]) || str[i-1] == ')')){
						in_s0('*');
					}

					in_s0(str[i]);
				}else{
					while (!s0.empty() && !s0.peek().equals("(")) {
						mult(s0.peek());
						s0.pop();
					}
					if (!s0.empty() && s0.peek().equals("(")) {
						s0.pop();
					}
					if (i != stri.length() - 1) {
						if (Utils.isdigit(stri.charAt(i + 1))) {
							in_s0('*');
						}
					}
				}
			}
		}
		while (!s0.isEmpty() && s1.size() >= 2) {
			mult(s0.peek());
			s0.pop();
		}
		if (!s0.isEmpty() && s1.size() == 1) {
			if (s0.peek().equals("R")) {
				mult(s0.peek());
				s0.pop();
			}
			if (!s0.isEmpty() && (s0.peek().equals("cos") || s0.peek().equals("sin") || s0.peek().equals("log") || s0.peek().equals("ln") || s0.peek().equals("tan"))) {
				mult(s0.peek());
				s0.pop();
			}
		}
		if(!was_error){
			coreLinkBridge.onSuccess(s1.peek(), type);
		}
	}

	private void mult(String x){
		if (x.length() == 3 || x.equals("ln") || x.equals("R")) {
			double d = s1.peek().doubleValue(), ans = 1;
			if (x.equals("log") && d <= 0) {
				//Toast.makeText(getApplicationContext(), "You cannot find the logarithm of a zero or a negative number.", Toast.LENGTH_SHORT).show();
				return;
			}
			s1.pop();
			switch (x) {
				case "cos": {
					ans = Math.cos(d);
					break;
				}
				case "sin": {
					ans = Math.sin(d);
					break;
				}
				case "tan": {
					ans = Math.tan(d);
					break;
				}
				case "log": {
					ans = Math.log10(d);
					break;
				}
				case "ln": {
					ans = Math.log(d);
					break;
				}
				case "R":
					ans = Math.sqrt(d);
					break;
			}

			BigDecimal ansb = BigDecimal.valueOf(ans);
			ansb = ansb.divide(BigDecimal.valueOf(1.0), 9, RoundingMode.HALF_EVEN);
			String answer = ansb.toPlainString();
			int len = answer.length();
			if (answer.charAt(len - 1) == '0' && answer.contains(".")) {
				while (len > 0 && answer.charAt(len - 1) == '0') {
					len--;
					answer = answer.substring(0, len);
				}
				if (answer.charAt(len - 1) == '.') {
					answer = answer.substring(0, len - 1);
				}
			}
			s1.push(new BigDecimal(answer));
			return;
		}
		BigDecimal b = s1.peek();
		s1.pop();
		BigDecimal a = s1.peek();
		BigDecimal ans = s1.peek();
		s1.pop();
		double a1, b1, ansd = 0.0;
		a1 = a.doubleValue();
		b1 = b.doubleValue();
		try{
			switch (x) {
				case "+":
					ansd = a1 + b1;
					break;
				case "-":
					ansd = a1 - b1;
					break;
				case "*":
					ansd = a1 * b1;
					break;
				case "/":
					if(b1 == 0){
						was_error = true;
						return;
					}
					ansd = a1 / b1;
					break;
				case "^":
					ansd = Math.pow(a1, b1);
					break;
			}
			ans = new BigDecimal(BigDecimal.valueOf(ansd).toPlainString());
			if(!ans.equals(BigDecimal.valueOf(0.0)) && !(ans.toString().contains("E")))
				ans = ans.divide(BigDecimal.valueOf(1.0), 9, RoundingMode.HALF_EVEN);

			String answer = ans.toPlainString();
			int len = answer.length();
			if(answer.charAt(len-1) == '0'){
				while (len > 0 && answer.charAt(len - 1) == '0') {
					len--;
					answer = answer.substring(0, len);
				}
				if(answer.charAt(len-1) == '.'){
					answer = answer.substring(0, len-1);
				}
				ans = new BigDecimal(answer);
			}
			s1.push(ans);
		}catch (ArithmeticException e){
			//Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
			String str = e.toString();
			if(str.contains("Non-terminating decimal expansion; no exact representable decimal result")){
				ans = a.divide(b, 3, RoundingMode.HALF_EVEN);
				s1.push(ans);
			}else{
				was_error = true;
				coreLinkBridge.onError(e.toString());
			}
		}catch(Exception e){
			was_error = true;
			coreLinkBridge.onError(e.toString());
		}

	}

	private void in_s0(char x){
		Map<String, Integer> priority = new HashMap<>();
		priority.put("(", 0);
		priority.put("-", 1);
		priority.put("+", 1);
		priority.put("/", 2);
		priority.put("*", 2);
		priority.put("^", 3);
		priority.put("R", 3);
		//Toast.makeText(getApplicationContext(), priority.get("(") + " " + priority.get("-"), Toast.LENGTH_SHORT).show();
		if(x == '('){
			s0.push(Character.toString(x));
			return;
		}
		if(s0.empty()){
			s0.push(Character.toString(x));
			return;
		}

		if(!s0.empty() && s0.peek().equals("(")){
			s0.push(Character.toString(x));
			return;
		}
		if (!s0.empty() && (priority.get(Character.toString(x)) < priority.get(s0.peek()) || priority.get(Character.toString(x)).equals(priority.get(s0.peek())))) {
			mult(s0.peek());
			s0.pop();
			in_s0(x);
			return;
		}
		if(!s0.empty() && priority.get(Character.toString(x)) > priority.get(s0.peek()))
			s0.push(Character.toString(x));
	}
	
}