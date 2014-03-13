package macsy.module.onlineClassification;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.blackBoardsSystem.BlackBoardDateBased;

/** 
 * This is a class that helps the main module which classifies 
 * the document to communicate with the blackboard
 * 
 * @author Panagiota Antonakaki
 * Last update: 12-03-2014
 *
 */
public class OnlineClassifier_StorageLayer {
	private BlackBoardDateBased inputbb = null;
	private BlackBoardDateBased outputbb = null;
	
	/**
	 * Initialise the blackboard
	 * @param bb
	 * @throws Exception
	 */
	public OnlineClassifier_StorageLayer(BlackBoardDateBased inputbb, BlackBoardDateBased outputbb) throws Exception
	{
		this.inputbb = inputbb;
		this.outputbb = outputbb;
	}		
	
	/**
	 * Get the set of docs with the specified tags (more than one)
	 * @param inputTag_List
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	public BBDocSet getDocSetWithTags(List<Integer> inputTag_List, 
			int limit) throws Exception
	{
		return inputbb.findDocsByTagsSet(inputTag_List, null, limit);
	}	
	
	/**
	 * For testing reasons only. Remove!
	 */
	public void getDocSetWithTagsTest() throws Exception
	{
		Calendar fromDay = new GregorianCalendar();
		fromDay.set(2013, 8, 13);
		
		Calendar toDay = new GregorianCalendar();	//Today
		toDay.add(Calendar.DATE,2);
		
		while( fromDay.before( toDay) )
		{
			int y = fromDay.get(Calendar.YEAR);
			int m = fromDay.get(Calendar.MONTH);
			int m1 = m+1;
			int d = fromDay.get(Calendar.DAY_OF_MONTH);
		
			Date fromDate = new GregorianCalendar(y,m,d,0,0,1).getTime();
			Date toDate = new GregorianCalendar(y,m,d,23,59,59).getTime();

			long c = inputbb.countDocs(fromDate, toDate, null, null);
			
			System.out.println(
					y+"-"+ 
					m1+"-"+
					d+"\t"+
					c);

			fromDay.add(Calendar.DATE,1);
		}
	}
	
	/**
	 * Get the set of docs with the specified tags (more than one) and the specified
	 * period of days
	 * @param inputTag_List
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	public BBDocSet getDocSetWithTags(Date fromDate, Date toDate,
			List<Integer> inputTag_List, 
			int limit) throws Exception
	{
		return inputbb.findDocsByFieldsTagsSet(fromDate, 
				toDate, 
				null, 
				null, 
				inputTag_List, 
				null, 
				limit);
	}	
	
	/**
	 * Find in the database the documents in the specific period of time
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	public BBDocSet findDocsByFieldsTagsSet(Date fromDate, 
			Date toDate, List<Integer> withTags,   
			int limit) throws Exception
	{
		return inputbb.findDocsByFieldsTagsSet(fromDate, 
				toDate, 
				null, 
				null, 
				withTags, 
				null, 
				limit);
	}

	/**
	 * Adds a tag on the specific document
	 * @param docID
	 * @param tagID
	 * @throws Exception
	 */
	public void addTagsToDoc(Object docID, Integer tagID) throws Exception
	{
//		List<Integer> tagIDs = new LinkedList<Integer>();
//		tagIDs.add(tagID);
//		outputbb.addTagsToDoc(docID, tagIDs);
	}
	
	/**
	 * Adds a field on the specific document
	 * @param docID
	 * @param fieldName
	 * @param fieldValue
	 * @throws Exception
	 */
	public void addFieldToDoc(Object docID, 
			String fieldName, 
			Double fieldValue) throws Exception
	{
		//outputbb.addFieldToDoc(docID, fieldName, fieldValue);
	}
	
	/**
	 * Removes tag(s) from the specific document
	 * @param feedID
	 * @param tagIDs
	 * @throws Exception
	 */
	public void removeTagsFromDoc(Object feedID, List<Integer> tagIDs) throws Exception
	{
		//inputbb.removeTagsFromDoc(feedID, tagIDs);
	}
	
	/**
	 * Returns a list with the tag ids of the document
	 * @param doc
	 * @return
	 */
	public List<Integer> getAllTagIDs(BBDoc doc){
		return doc.getAllTagIDs();
	}
	
	/**
	 * Returns the id of the specific tag of 0 if it doesn't exist
	 * @param tagName
	 * @return
	 * @throws Exception
	 */
	public int getInputTagID(String tagName) throws Exception
	{
		int tagID =  inputbb.getTagID(tagName);
		if(tagID == BlackBoard.TAG_NOT_FOUND)
			tagID = 0;
		return tagID;
	}
	
	/**
	 * Returns the tag id of the specific tag
	 * (insert the tag if it doesn't exist)
	 * @param tagName
	 * @return
	 * @throws Exception
	 */
	public int getOutputTagID(String tagName) throws Exception
	{
		int tagID =  inputbb.getTagID(tagName);
		if(tagID == BlackBoard.TAG_NOT_FOUND)
			tagID = inputbb.insertNewTag( tagName );
		return tagID;
	}

        public static void main(String args[]){

        }

        /*
        	 * Finds a set of articles gathered from a given feed.
	 *
	 * Retrieves articles with dates: fromDate <= article_date < toDate
	 *
	 * @param listFieldName : The list of names of the fields of interest.
	 * @param listFieldValue : The list of values of the fields of interest.
	 * @param fromDate : The start of the time period of interest.
	 * @param toDate : The end of the time period of interest.
	 * @param withTagIDs : A list of type  List<Integer> with the IDs of the tags that articles should carry.
	 * @param withoutTagIDs : A list of type  List<Integer> with the IDs of the tags that articles should not carry.
	 * @param resSize : The maximum size of articles to return - Max is set to MAX_ARICLEIDS_IN_RESULT_LIST.
	 * @return A list with IDs of Articles
	 * @throws Exception
	 */
	public BBDocSet findDocsWithValueInListSet(
			Date fromDate,
			Date toDate,
			String listFieldName,
			Object listFieldValue,
			List<Integer> withTagIDs,
			List<Integer> withoutTagIDs,
			int resSize
			) throws Exception
			{
            return inputbb.findDocsWithValueInListSet(fromDate, toDate, listFieldName, listFieldValue, withTagIDs, withoutTagIDs, resSize);

			}
}
