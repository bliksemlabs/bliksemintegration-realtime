package nl.ovapi.bison.model;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString()
public class KV17cvlinfo {

	@Getter
	@Setter
	private DataOwnerCode dataownercode;
	@Getter
	@Setter
	private String lineplanningnumber;
	@Getter
	@Setter
	private Integer journeynumber;
	@Getter
	@Setter
	private Integer reinforcementnumber;
	@Getter
	@Setter
	private String operatingday;
	@Getter
	@Setter
	private Long timestamp;

	@Getter
	@Setter
	private ArrayList<Mutation> mutations;

	public KV17cvlinfo() {
		mutations = new ArrayList<Mutation>();
	}

	@ToString()
	public static class Mutation {
		public enum MessageType {
			KV17MUTATEJOURNEY, KV17MUTATEJOURNEYSTOP
		}

		public enum MutationType {
			CANCEL, RECOVER, SHORTEN, LAG, CHANGEPASSTIMES, CHANGEDESTINATION, MUTATIONMESSAGE;
		}

		@Getter
		@Setter
		private String userstopcode;
		@Getter
		@Setter
		private Integer passagesequencenumber;
		@Getter
		@Setter
		private MessageType messagetype;
		@Getter
		@Setter
		private MutationType mutationtype;
		@Getter
		@Setter
		private String reasontype;
		@Getter
		@Setter
		private String subreasontype;
		@Getter
		@Setter
		private String reasoncontent;
		@Getter
		@Setter
		private String advicetype;
		@Getter
		@Setter
		private String subadvicetype;
		@Getter
		@Setter
		private String advicecontent;
		@Getter
		@Setter
		private Integer lagtime;
		@Getter
		@Setter
		private Integer targetarrivaltime;
		@Getter
		@Setter
		private Integer targetdeparturetime;
		@Getter
		@Setter
		private JourneyStopType journeystoptype;
		@Getter
		@Setter
		private String destinationcode;
		@Getter
		@Setter
		private String destinationname50;
		@Getter
		@Setter
		private String destinationname16;
		@Getter
		@Setter
		private String destinationdetail16;
		@Getter
		@Setter
		private String destinationdisplay16;
	}
}
