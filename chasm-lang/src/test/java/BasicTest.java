import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quiltmc.chasm.lang.Evaluator;
import org.quiltmc.chasm.lang.Scope;
import org.quiltmc.chasm.lang.ast.IntegerExpression;
import org.quiltmc.chasm.lang.ast.StringExpression;
import org.quiltmc.chasm.lang.op.Expression;
import org.quiltmc.chasm.lang.op.ListExpression;

public class BasicTest {
    @Test
    public void recursionTest() {
        String test = """
                {
                    run: state -> state.count = 0 ? "Done" : run({ count: state.count - 1 })
                }
                """;

        Evaluator evaluator = new Evaluator();
        Expression parsedTest = Expression.parse(CharStreams.fromString(test));
        Expression resolvedTest = evaluator.resolve(parsedTest);
        evaluator.getScope().push(Scope.singleton("test", resolvedTest));

        Expression parsed = Expression.parse(CharStreams.fromString("test.run({count: 10})"));
        Expression resolved = evaluator.resolve(parsed);
        Expression reduced = evaluator.reduce(resolved);

        Assertions.assertInstanceOf(StringExpression.class, reduced);
        Assertions.assertEquals("Done", ((StringExpression) reduced).getValue());
    }

    @Test
    public void reverseResolve() {
        String test = """
                {
                    val: 1,
                    inner: {
                        val: 0,
                        result: $val - val
                    }
                }
                """;

        Evaluator evaluator = new Evaluator();
        Expression parsedTest = Expression.parse(CharStreams.fromString(test));
        Expression resolvedTest = evaluator.resolve(parsedTest);
        evaluator.getScope().push(Scope.singleton("test", resolvedTest));

        Expression parsed = Expression.parse(CharStreams.fromString("test.inner.result"));
        Expression resolved = evaluator.resolve(parsed);
        Expression reduced = evaluator.reduce(resolved);

        Assertions.assertInstanceOf(IntegerExpression.class, reduced);
        Assertions.assertEquals(1, ((IntegerExpression) reduced).getValue());
    }

    @Test
    public void recursionTest2() {
        String test = """
                {
                    run: state -> state = 0 ? "Done" : run(state - 1)
                }
                """;

        Evaluator evaluator = new Evaluator();
        Expression parsedTest = Expression.parse(CharStreams.fromString(test));
        Expression resolvedTest = evaluator.resolve(parsedTest);
        evaluator.getScope().push(Scope.singleton("test", resolvedTest));

        Expression parsed = Expression.parse(CharStreams.fromString("test.run(10)"));
        Expression resolved = evaluator.resolve(parsed);
        Expression reduced = evaluator.reduce(resolved);

        Assertions.assertInstanceOf(StringExpression.class, reduced);
        Assertions.assertEquals("Done", ((StringExpression) reduced).getValue());
    }

    @Test
    public void parseAndRun() {
        String test = """
                {
                    int: 5 + 3,
                    bool: true,
                    string: "abc",
                    ref: int,
                    self: $this.int,
                    lambda: arg -> arg + 2,
                    call: lambda(4),
                    ternary: false ? "true" : "false",
                    equals: 5 = 3,
                    fibonacci: val -> val = 1 ? 1 : val = 2 ? 1 : fibonacci(val - 1) + fibonacci(val - 2),
                    call_fib: fibonacci(46),
                    curry: first -> second -> first - second,
                    call_curry: curry(5)(3),
                    list: [1, "two", false, { name: "object" }, null],
                    list_index: list[1],
                    map_member: list[3].name,
                    map_index: list[3]["name"],
                    concat: [1, 2] + [3, 4],
                    list_concat: [1, 2] + list,
                    filter_source: [{name: "hi"}, {name: "how"}, {name: "are"}, {name: "you"}],
                    filter: filter_source[entry -> entry.name[0] = "h"],
                    compareWeird: arg -> arg > 3 = arg < 5 * -arg + 1000,
                    test: arg -> arg.a && arg.b || arg.c && arg.d,
                    test_call: test({ a: true, b: false, c: true, d: true }),
                    binop: x -> (x & ~7) | 1,
                    binop_call: binop(125),
                    shift: x -> x << 1,
                    shift_call: shift(125),
                    shifting: x -> x << 1 ^ x >> 1,
                    xmas: "Hello " + "World" + "!"
                }
                """;

        Evaluator evaluator = new Evaluator();
        Expression parsed = Expression.parse(CharStreams.fromString(test));
        Expression resolved = evaluator.resolve(parsed);
        Expression reduced = evaluator.reduceRecursive(resolved);

        System.out.println(reduced);
    }

    @Test
    public void testBrainfuck() {
        // Simple brainfuck implementation and program
        String test = """
                {
                    data_size: 20,
                    init_list: args -> args.length = 0 ? [] :
                        [args.value] + init_list({value: args.value, length: args.length - 1}),
                    init: {
                        ptr: 0,
                        data: init_list({
                            value: 0,
                            length: data_size
                        }),
                        pc: 0,
                        program: "
                            ++++++++++[>+++++++>++++++++++>+++>+<<<<-]
                            >++.>+.+++++++..+++.>++.<<+++++++++++++++.
                            >.+++.------.--------.>+.",
                        out: []
                    },
                    set: args ->
                        args.start = args.length ? args.result : set({
                            start: args.start + 1,
                            result: args.result + [args.start = args.index ? args.value : args.list[args.start]],
                            list: args.list,
                            length: args.length,
                            index: args.index,
                            value: args.value
                        }),
                    jmp_forward: args ->
                        args.depth = 0 ? args.pc :
                        args.program[args.pc] = "[" ? jmp_forward({
                            depth: args.depth + 1,
                            program: args.program,
                            pc: args.pc + 1
                        }) :
                        args.program[args.pc] = "]" ? jmp_forward({
                            depth: args.depth - 1,
                            program: args.program,
                            pc: args.pc + 1
                        }) :
                        jmp_forward({
                            depth: args.depth,
                            program: args.program,
                            pc: args.pc + 1
                        }),
                    jmp_back: args ->
                        args.depth = 0 ? args.pc + 2 :
                        args.program[args.pc] = "[" ?  jmp_back({
                            depth: args.depth - 1,
                            program: args.program,
                            pc: args.pc - 1
                        }) :
                        args.program[args.pc] = "]" ? jmp_back({
                            depth: args.depth + 1,
                            program: args.program,
                            pc: args.pc - 1
                        }) :
                        jmp_back({
                            depth: args.depth,
                            program: args.program,
                            pc: args.pc - 1
                        }),
                    run: state ->
                        state.program[state.pc] = null ? state.out :
                        state.program[state.pc] = ">" ? run({
                            ptr: state.ptr + 1,
                            data: state.data,
                            pc: state.pc + 1,
                            program: state.program,
                            out: state.out
                        }) :
                        state.program[state.pc] = "<" ? run({
                            ptr: state.ptr - 1,
                            data: state.data,
                            pc: state.pc + 1,
                            program: state.program,
                            out: state.out
                        }) :
                        state.program[state.pc] = "." ? run({
                            ptr: state.ptr,
                            data: state.data,
                            pc: state.pc + 1,
                            program: state.program,
                            out: state.out + [state.data[state.ptr]]
                        }) :
                        state.program[state.pc] = "+" ? run({
                            ptr: state.ptr,
                            data: set({
                                start: 0,
                                result: [],
                                list: state.data,
                                length: data_size,
                                index: state.ptr,
                                value: state.data[state.ptr] + 1
                            }),
                            pc: state.pc + 1,
                            program: state.program,
                            out: state.out
                        }) :
                        state.program[state.pc] = "-" ? run({
                            ptr: state.ptr,
                            data: set({
                                start: 0,
                                result: [],
                                list: state.data,
                                length: data_size,
                                index: state.ptr,
                                value: state.data[state.ptr] - 1
                            }),
                            pc: state.pc + 1,
                            program: state.program,
                            out: state.out
                        }) :
                        state.program[state.pc] = "[" ? run({
                            ptr: state.ptr,
                            data: state.data,
                            pc: state.data[state.ptr] = 0 ?
                                jmp_forward({
                                    depth: 1,
                                    pc: state.pc + 1,
                                    program: state.program
                                }) :
                                state.pc + 1,
                            program: state.program,
                            out: state.out
                        }) :
                        state.program[state.pc] = "]" ? run({
                            ptr: state.ptr,
                            data: state.data,
                            pc: state.data[state.ptr] = 0 ?
                                state.pc + 1 :
                                jmp_back({
                                    depth: 1,
                                    pc: state.pc - 1,
                                    program: state.program
                                }),
                            program: state.program,
                            out: state.out
                        }) :
                        run({
                            ptr: state.ptr,
                            data: state.data,
                            pc: state.pc + 1,
                            program: state.program,
                            out: state.out
                        }),
                    result: run(init)
                }
                """;

        Evaluator evaluator = new Evaluator();
        Expression parsedTest = Expression.parse(CharStreams.fromString(test));
        Expression resolvedTest = evaluator.resolve(parsedTest);
        evaluator.getScope().push(Scope.singleton("test", resolvedTest));

        Expression parsed = Expression.parse(CharStreams.fromString("test.result"));
        Expression resolved = evaluator.resolve(parsed);
        Expression reduced = evaluator.reduceRecursive(resolved);

        StringBuilder result = new StringBuilder();
        Assertions.assertInstanceOf(ListExpression.class, reduced);
        for (Expression entry : (ListExpression) reduced) {
            Assertions.assertInstanceOf(IntegerExpression.class, entry);
            result.append((char) ((IntegerExpression) entry).getValue().intValue());
        }
        Assertions.assertEquals("Hello World!", result.toString());
    }
}
