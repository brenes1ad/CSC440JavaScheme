import java.util.List;

/**
 * A user-defined Scheme procedure (lambda).
 * Stores its formal parameter list, body expression, and the
 * lexical environment in which it was created (closure).
 */
public class LambdaFun {

    /** Formal parameter names, e.g. ["x", "y"] */
    public final List<Object> parms;

    /** The body expression to evaluate when called. */
    public final Object body;

    /** The static (lexical) environment captured at definition time. */
    public final Env env;

    /**
     * @param parms the formal parameter list
     * @param body  the body expression
     * @param env   the enclosing environment at definition time
     */
    public LambdaFun(List<Object> parms, Object body, Env env) {
        this.parms = parms;
        this.body  = body;
        this.env   = env;
    }

    /**
     * Invoke the lambda with the given arguments.
     * Creates a fresh child environment binding parms -> args,
     * then delegates to JSchemeApp.eval.
     *
     * @param args actual argument values
     * @return the result of evaluating the body
     */
    public Object call(List<Object> args) {
        return JSchemeApp.eval(body, new Env(parms, args, env));
    }

    @Override
    public String toString() {
        return "#<procedure>";
    }
}
