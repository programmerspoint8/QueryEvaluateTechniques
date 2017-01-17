
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class IRP02 {
	
	static File file;
	static FileWriter fw;
	static Writer bw;
	
	static List<List<Integer>> positionsArrayTaatAnd = new ArrayList<List<Integer>>();
	static List<List<Integer>> positionsArrayTaatOr = new ArrayList<List<Integer>>();
	static List<List<Integer>> positionsArrayDaatAnd = new ArrayList<List<Integer>>();
	static List<List<Integer>> positionsArrayDaatOr = new ArrayList<List<Integer>>();

	public static void main(String[] args) throws IOException {
		
		String path_of_index = args[0];
		String inputFileName = args[2];
		String outputFileName = args[1];

//		String path_of_index = "C:\\Users\\Jay\\Downloads\\Phone_Stuff\\New folder\\535 Information Retrieval\\Project-02\\index";
//		String inputFileName = "input1.txt";
//		String outputFileName = "output.txt";
	
		
		file = new File(outputFileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
		
		positionsArrayTaatAnd = new ArrayList<List<Integer>>();
		positionsArrayTaatOr = new ArrayList<List<Integer>>();
		positionsArrayDaatAnd = new ArrayList<List<Integer>>();
		positionsArrayDaatOr = new ArrayList<List<Integer>>();
		
		HashMap<String,LinkedList<Integer>> hm = createPostings(path_of_index);
        String query = "";

        try {
        	File fileInputDir = new File(inputFileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileInputDir), "UTF8"));
            while((query = bufferedReader.readLine()) != null) {
                GetPostings(hm, query);
            	TaatAnd(query, positionsArrayTaatAnd);
            	TaatOr(query, positionsArrayTaatOr);
            	DaatAnd(query, positionsArrayDaatAnd);
            	DaatOr(query, positionsArrayDaatOr);
            	
            	positionsArrayTaatAnd.clear();
        		positionsArrayTaatOr.clear();
        		positionsArrayDaatAnd.clear();
        		positionsArrayDaatOr.clear();
            	
            }   
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            bw.write("Unable to open file '" + inputFileName + "'");                
        }
        catch(IOException ex) {
            bw.write("Error reading file");
        }
		
		bw.close();
		
	}

	static HashMap<String,LinkedList<Integer>> createPostings(String path_of_index) throws IOException{
		Directory dirIndex = FSDirectory.open(Paths.get(path_of_index));
		IndexReader indexReader = DirectoryReader.open(dirIndex);
		
		Fields fields = MultiFields.getFields(indexReader);			// Extracting Field Names
		ArrayList<String> fieldNames = new ArrayList<String>();
		for(String field : fields){
			if(field.indexOf("text_")>-1)
				fieldNames.add(field);								// Filtering Required Field Names
		}
		
        HashMap<String,LinkedList<Integer>> hm = new HashMap<String,LinkedList<Integer>>();
		
		for(String field : fieldNames){
			Terms terms = fields.terms(field);						// Extracting terms for particular field
	        TermsEnum iterator = terms.iterator();
	        BytesRef byteRef = null;
	        while((byteRef = iterator.next()) != null) {
	            String st = byteRef.utf8ToString();
	            LinkedList<Integer> postings = new LinkedList<Integer>();
	            PostingsEnum poEnum = null;
	            poEnum = iterator.postings(poEnum);					// Retreiving Postings for particular term
	            int position;
				while((position = poEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS){
	                postings.add(position);							// Adding the values of postings to LinkedList 
				}
	            hm.put(st, postings);								// Adding to hashmap, key: term, value: List of postings
	        }
		}
		
        return hm;
	}	
	
	static void GetPostings(HashMap<String, LinkedList<Integer>> hm, String query) throws IOException {
		String terms[] = query.split(" ");
		for(String term : terms){
			bw.write("GetPostings"+" \n");
			bw.write(term+" \n");
			bw.write("Postings list: ");
			if(hm.containsKey(term)){
				LinkedList<Integer> postingsList = hm.get(term);				// Retreiving postings for terms in query
				positionsArrayTaatAnd.add(postingsList);
				positionsArrayTaatOr.add(postingsList);
				positionsArrayDaatAnd.add(postingsList);
				positionsArrayDaatOr.add(postingsList);
				for(Integer position: postingsList){
					bw.write(position+" ");										// Printing postings to file
				}
			}
			bw.write("\n");
		}
	}
	
	static void TaatAnd(String query, List<List<Integer>> posArray) throws IOException {
		bw.write("TaatAnd \n");
		String terms[] = query.split(" ");
		for(String term : terms){
			bw.write(term+" ");
		}
		bw.write("\n");
		bw.write("Results: ");
		
		int minIndex = 0, compareCount=0;
		boolean isFirst = true;														// flag variable to note addition of first term
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		int size = posArray.size();
		while(size>0){
			minIndex = 0;															// Pointing to next available term's postings list 
			
			/*
			minSize = posArray.get(minIndex).size();
			// Finding index of array having least size
			for(int i=0; i<size; i++){
				if(minSize > posArray.get(i).size()){
					minSize = posArray.get(i).size();
					minIndex = i;
				}
			}
			*/
			
			if(isFirst){															// Initial addition of postings of first term to intermediate result
				for(int i=0; i < posArray.get(minIndex).size(); i++){
					results.add(posArray.get(minIndex).get(i));
				}
				posArray.remove(minIndex);											// Removing postings list of particular term after parsing a postings list of particular term
				isFirst = false;													// Changing of flag variable, after first term's postings list is parsed
			}
			else{
				int ai, aj=0, check = -1;											// ai to keep pointer to results list, aj to keep pointer current term's postings list
				int resultsSize = results.size();
				int temp = posArray.get(minIndex).size();							// temp to keep size of current term's postings list
				for(ai=0; ai<resultsSize; ai++){
					int result = results.get(ai);									// assigning (ai)th element of results list to result 
					if(aj<temp){
						check = (int)((posArray.get(minIndex)).get(aj));			// assigning (aj)th element of current terms's postings list to check
						aj++;
						for(; aj < temp && result > check; ){
							// Comparisions
							//System.out.println("Comparing here "+result+" "+check);
							check = (int)((posArray.get(minIndex)).get(aj++));
							compareCount++;
						}
						compareCount++;
						if(aj != 0)
							aj--;
					
						if(result != (int)((posArray.get(minIndex)).get(aj))){
							results.remove(ai);										// removing (ai)th element of results if not found in current term's postings list
							resultsSize = results.size();
							ai--;
						}
					}
					else{
						results.remove(ai);											// removing (ai)th element of results if not found in current term's postings list
						resultsSize = results.size();
						ai--;
					}
				}
				
				posArray.remove(minIndex);
			}
			size = posArray.size();
		}
		if(results.size() == 0){
			bw.write("empty ");
		}
		for(int result : results){
			bw.write(result+" ");
		}
		bw.write("\n");
		//System.out.println("Number of documents in results: "+results.size()+" \n");
		bw.write("Number of documents in results: "+results.size()+" \n");
		bw.write("Number of comparisons: "+ compareCount + " \n");
	}
	
	static void TaatOr(String query, List<List<Integer>> posArray) throws IOException {
		bw.write("TaatOr \n");
		int compareCount = 0;
		String terms[] = query.split(" ");
		for(String term : terms){
			bw.write(term+" ");
		}
		bw.write("\n");
		bw.write("Results: ");
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		results.clear();
		ArrayList<Integer> tempResults = new ArrayList<Integer>();
		int size = posArray.size();
		
		for(int i=0; i<size; i++){
			if(i==0){
				for(int j=0; j < posArray.get(i).size(); j++){
					results.add(posArray.get(i).get(j));
				}
			}
			else{
				tempResults.clear();
				tempResults.addAll(results);
				results.clear();
				int newElement=-1, oldElement=-1;
				int j=0, k=0;
				
				// Merging postings list of current and temporary results which is initially empty
				for(j=0, k=0; j<posArray.get(i).size() && k<tempResults.size(); ){
					
					newElement = posArray.get(i).get(j);
					oldElement = tempResults.get(k);
					
					if(newElement < oldElement){
						results.add(newElement);
						compareCount++;
						j++;
					}
					else if(newElement > oldElement){
						results.add(oldElement);
						compareCount++;
						k++;
					}
					else if(newElement == oldElement){
						results.add(oldElement);
						compareCount++;
						k++;
						j++;
					}
				}
				
				// Adding of remaining terms
				for(;j<posArray.get(i).size();){
					newElement = posArray.get(i).get(j);
					results.add(newElement);
					j++;
				}
				for(;k<tempResults.size();){
					oldElement = tempResults.get(k);
					results.add(oldElement);
					k++;
				}
				
			}
		}
		if(results.size() == 0){
			bw.write("empty ");
		}
		for(int result : results){
			bw.write(result+" ");
		}
		bw.write("\n");
		bw.write("Number of documents in results: "+results.size()+" \n");
		bw.write("Number of comparisons: "+ compareCount + " \n");
	}

	static void DaatAnd(String query, List<List<Integer>> posArray) throws IOException {
		bw.write("DaatAnd \n");
		String terms[] = query.split(" ");
		for(String term : terms){
			bw.write(term+" ");
		}
		bw.write("\n");
		bw.write("Results: ");
		
		int compareCount=0;
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		
		int size = posArray.size();
		
		int termPtr[] = new int[size];																
		
		for(int i=0; i<size; i++){
			termPtr[i] = 0;
		}
		boolean flag = true, found = false;
		//int k = 0;
		if(posArray.size()>0){
		for(int i=0; i<posArray.get(0).size(); i++){
			int docId = posArray.get(0).get(i);
			flag = true;																// considering docId is found in other terms' postings list
			for(int j=1; j<size && flag; j++){
				found = false;
				for(int k=termPtr[j]; k<posArray.get(j).size(); k++){
					int tDocId = posArray.get(j).get(k);
					if(tDocId == docId){
						termPtr[j] = k;
						compareCount++;
						found = true;
						break;
					}
					else if(tDocId > docId){
						termPtr[j] = k;
						flag = false;													// indicating that posting not found
						compareCount++;
						break;
					}
					else{
						compareCount++;
					}
				}
				if(found == false)
					flag = false;
			}
			if(flag == true){
				results.add(docId);
			}
		}
		}
		
		if(results.size() == 0){
			bw.write("empty ");
		}
		for(int result : results){
			bw.write(result+" ");
		}
		bw.write("\n");
		bw.write("Number of documents in results: "+results.size()+" \n");
		bw.write("Number of comparisons: "+ compareCount + " \n");
	}
	
	static void DaatOr(String query, List<List<Integer>> posArray) throws IOException {
		bw.write("DaatOr \n");
		String terms[] = query.split(" ");
		for(String term : terms){
			bw.write(term+" ");
		}
		bw.write("\n");
		bw.write("Results: ");
		
		int compareCount=0;
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		ArrayList<Integer> remaining = new ArrayList<Integer>();
		int size = posArray.size();
		
		int termPtr[] = new int[size];
		
		for(int i=0; i<size; i++){
			termPtr[i] = 0;
		}
		
		if(posArray.size()>0){
		for(int i=0; i<posArray.get(0).size(); i++){
			int docId = posArray.get(0).get(i);
			for(int j=1; j<size; j++){
				for(int k=termPtr[j]; k<posArray.get(j).size(); k++){
				//for(int k=0; k<posArray.get(j).size(); k++){
					int tDocId = posArray.get(j).get(k);
					if(tDocId == docId){
						termPtr[j] = k;
						compareCount++;
						remaining.add(tDocId);
						break;
					}
					else if(tDocId > docId){
						termPtr[j] = k;
						compareCount++;
						remaining.add(tDocId);
						break;
					}
					else{
						remaining.add(tDocId);
						compareCount++;
					}
				}
			}
			results.add(docId);
		}
		}
		
		// Adding remaining tail-end postings to remaining
		for(int j=1; j<size; j++){
			for(int k=termPtr[j]; k<posArray.get(j).size(); k++){
			//for(int k=0; k<posArray.get(j).size(); k++){
				int tDocId = posArray.get(j).get(k);
				remaining.add(tDocId);
			}
		}
		
		results.addAll(remaining);
		Collections.sort(results);
		
		int ressize = results.size();
		
		// Removing Duplicates
		for(int i=0; i<ressize-1; i++){
			int a = results.get(i);
			int b = results.get(i+1);
			if(a == b){
				results.remove(i);
				ressize = results.size();
				i--;
			}
		}
		
		if(results.size() == 0){
			bw.write("empty ");
		}
		for(int result : results){
			bw.write(result+" ");
		}
		bw.write("\n");
		bw.write("Number of documents in results: "+results.size()+" \n");
		bw.write("Number of comparisons: "+ compareCount + " \n");
	}
	
}