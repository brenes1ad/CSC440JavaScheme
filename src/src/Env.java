import java.util.HashMap;

/**
 * An environment: a hash map of {variable -> value} pairs,
 * with a link to an outer (parent) environment.
 */
public class Env extends HashMap<String, Object> {

    public final Env outer;

    /** Create a top-level environment with no parent. */
    public Env() {
        this.outer = null;
    }

    /**
     * Create a new child environment by binding formal parameters to argument values.
     *
     * @param parms list of parameter names
     * @param args  list of argument values (must match parms in length)
     * @param outer the enclosing environment
     */
    public Env(java.util.List<Object> parms, java.util.List<Object> args, Env outer) {
        this.outer = outer;
        for (int i = 0; i < parms.size(); i++) {
            put((String) parms.get(i), args.get(i));
        }
    }

    /**
     * Find the innermost environment where {@code var} is defined.
     *
     * @param var the variable name to look up
     * @return the environment containing {@code var}
     * @throws RuntimeException if the variable is not found in any scope
     */
    public Env find(String var) {
        if (containsKey(var)) {
            return this;
        } else if (outer != null) {
            return outer.find(var);
        } else {
            throw new RuntimeException("Undefined symbol: " + var);
        }
    }
}
