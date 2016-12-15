/** @file PlayerRole.java
 * Strategic player roles.
 *
 * @author Team F(utility)
 */



/**
 * A class for housing information on player roles.
 */
public class PlayerRole {
    
    /**
     * The various player roles.
     */
    public enum Role {
        LATERAL_DIREITO,
        LATERAL_ESQUERDO,
        ZAGUEIRO_DIREITO,
        ZAGUEIRO_ESQUERDO,
        PONTA_DIREITA,
        PONTA_ESQUERDA,
        MEIA_ESQUERDA,
        MEIA_DIREITA,
        ATACANTE_DIREITO,
        ATACANTE_ESQUERDO,
        GOLEIRO
    }
    
    /**
     * Returns an indication of whether a player with a given role is a defender.
     * 
     * @param role the player's role
     * @return true if the player is a defender
     */
    public static final boolean isDefender(Role role) {
    	return role == Role.MEIA_ESQUERDA || role == Role.MEIA_DIREITA || role == Role.ATACANTE_DIREITO;
    }
    
    /**
     * Returns an indiciation of whether a player with a given role is a wing.
     * 
     * @param role the player's role
     * @return true if the player is a wing
     */
    public static final boolean isWing(Role role) {
        return role == Role.LATERAL_DIREITO || role == Role.LATERAL_ESQUERDO;
    }
    
    /**
     * @param role in question
     * @return true if the role is an offensive position 
     */
    public static boolean isOnOffense(Role role){
    	final boolean onD = PlayerRole.isDefender(role);
    	return !onD && role != Role.GOLEIRO;
    }
}
