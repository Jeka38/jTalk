package org.jivesoftware.smackx.muc;

/**
 * Default implementation of the ParticipantStatusListener interface.<p>
 *
 * This class does not provide any behavior by default. It just avoids having
 * to implement all the inteface methods if the user is only interested in implementing
 * some of the methods.
 * 
 * @author Gaston Dombiak
 */
public class DefaultParticipantStatusListener implements ParticipantStatusListener {
	
	public void statusChanged(String participant, String id) {
	}

    public void joined(String participant, String id) {
    }

    public void left(String participant, String id) {
    }

    public void kicked(String participant, String actor, String reason, String id) {
    }

    public void voiceGranted(String participant, String id) {
    }

    public void voiceRevoked(String participant, String id) {
    }

    public void banned(String participant, String actor, String reason, String id) {
    }

    public void membershipGranted(String participant, String id) {
    }

    public void membershipRevoked(String participant, String id) {
    }

    public void moderatorGranted(String participant, String id) {
    }

    public void moderatorRevoked(String participant, String id) {
    }

    public void ownershipGranted(String participant, String id) {
    }

    public void ownershipRevoked(String participant, String id) {
    }

    public void adminGranted(String participant, String id) {
    }

    public void adminRevoked(String participant, String id) {
    }

    public void nicknameChanged(String participant, String newNickname, String id) {
    }

}
