
/// <reference types="tree-sitter-cli/dsl" />
// @ts-check

const DIGITS = repeat1(/[0-9]/);
const DECIMAL_INTEGER = choice('0', seq(/[1-9]/, optional(DIGITS)));
const HEX_DIGITS = repeat1(/[0-9a-fA-F]/);
const HEX_INTEGER = seq('0x', HEX_DIGITS);
const FLOAT_EXPONENT = seq(choice('e', 'E'), optional(choice('+', '-')), DIGITS);

const intLiteral = choice(DECIMAL_INTEGER, HEX_INTEGER);
const floatLiteral = choice(
    seq(DECIMAL_INTEGER, '.', optional(DIGITS), optional(FLOAT_EXPONENT), optional(choice('f', 'F'))),
    seq(DECIMAL_INTEGER, FLOAT_EXPONENT, optional(choice('f', 'F'))),
    seq('.', DIGITS, optional(FLOAT_EXPONENT), optional(choice('f', 'F'))),
);
const stringLiteral = choice(
    seq('"', repeat(choice(/[^"]/, /\\./)), '"'),
    seq("'", repeat(choice(/[^']/, /\\./)), "'",),
);


const identifier = /[\p{L}_$][\p{L}\p{Nd}\u00A2_$]*/;

const comment = choice(
    seq("//", /.*/),

    seq(
        '/*',
        /[^*]*\*+([^/*][^*]*\*+)*/,
        '/',
    ),
);
const preprocessor = seq("#", /.*/);


function commaSep1(rule) {
    return seq(rule, repeat(seq(',', rule)));
}

function commaSep(rule) {
    return optional(commaSep1(rule));
}

module.exports = grammar({
    name: "zenscript",
    word: $ => $._identifier,

    extras: $ => [
        $.comment,
        $.preprocessor,
        /\s/,
    ],


    precedences: $ => [
        [
            'name',
            'parens',
            'literal',
            'member',
            'range',
            'call',
            'index',
            'as',
            'unary',
            'mul',
            'add',
            'compare',
            'and',
            'xor',
            'or',
            'and_and',
            'or_or',
            'conditional',
            'assign',
        ],

        [
            'array_type',
            'map_type',
            'as',
        ],

        [
            $.block_statement,
            $.map_literal
        ]
    ],

    inline: $ => [
        $._argument_list,
        $._type_list,
        $.statement,
        $._operator,
    ],

    // conflicts: $ => [
    //     [$.class_declaraton],
    // ],

    rules: {
        script_file: $ => seq(
            repeat($.import_declaraton),
            repeat($._top_level_element)
        ),

        _top_level_element: $ => choice(
            $.function_declaraton,
            $.expand_function_declaraton,
            $.class_declaraton,
            $.statement
        ),

        _class_member: $ => choice(
            $.function_declaraton,
            $.field_declaration,
            $.constructor_declaration,
            $.operator_function_declaration,
        ),

        import_declaraton: $ => seq('import', $.qualified_name, optional(seq('as', $.simple_name)), ';'),

        function_declaraton: $ => choice(

            seq('function',
                $.simple_name,
                '(',
                alias(commaSep($._parameter_decl), $.parameter_list),
                ')',
                optional(seq('as', $._type)),
                $.function_body,
            ),

            // dzs
            seq(optional(choice('static', 'global')),
                'function',
                optional($.simple_name),
                '(',
                alias(seq(commaSep($._parameter_decl), optional(seq(',', '...', $._parameter_decl))), $.parameter_list),
                ')',
                optional(seq('as', $._extend_type)),
                ';'
            ),

        ),

        field_declaration: $ => choice(

            seq(
                choice('var', 'val', 'static'),
                $.simple_name,
                optional(seq('as', $._type)),
                optional(seq('=', $._expression)),
                ';',
            ),



            seq(
                choice('var', 'val', 'static'),
                $.simple_name,
                optional(seq('as', $._extend_type)),
                ';',
            ),



        ),

        constructor_declaration: $ => choice(
            seq(
                'zenConstructor',
                '(',
                alias(commaSep($._parameter_decl), $.parameter_list),
                ')',
                $.function_body,
            ),

            // dzs
            seq(
                'zenConstructor',
                '(',
                alias(seq(commaSep($._parameter_decl), optional(seq(',', '...', $._parameter_decl))), $.parameter_list),
                ')',
                ';',
            ),
        ),

        expand_function_declaraton: $ => seq('$expand',
            $._type, '$', $.simple_name,
            '(', alias(commaSep($._parameter_decl), $.parameter_list), ')',
            optional(seq('as', $._type)),
            $.function_body
        ),

        // dzs
        operator_function_declaration: $ => seq(
            'operator',
            $._operator,
            '(', alias(commaSep($._parameter_decl), $.parameter_list), ')',
            optional(seq('as', $._extend_type)),
            ';'
        ),

        _operator: $ => choice(
            '+',
            '-',
            '*',
            '/',
            '%',
            '~',
            '|',
            '&',
            '^',
            '!',
            seq('[', ']'),
            seq('[', ']', '='),
            '..',
            'has',
            '.',
            seq('.', '='),
            'for_in',
            'as',
            '==',
            '!=',
            '<',
            '<=',
            '>',
            '>='
        ),

        class_declaraton: $ => choice(
            seq('zenClass',
                alias($.simple_name, $.class_name),
                $.class_body
            ),

            // dzs
            seq('zenClass',
                alias(choice(
                    $.simple_name,
                    $._primitive_type,
                ), $.class_name),
                optional(seq('extends', commaSep1($.qualified_name))),
                $.class_body
            ),
        ),

        function_body: $ => seq(
            '{',
            repeat($.statement),
            '}'
        ),

        class_body: $ => seq(
            '{',
            repeat($._class_member),
            '}'
        ),

        _parameter_decl: $ => seq($.simple_name, optional(seq('as', $._type)), optional(seq('=', $._expression))),

        statement: $ => choice(
            $.block_statement,
            $.return_statement,
            $.break_statement,
            $.continue_statement,
            $.if_statement,
            $.foreach_statement,
            $.while_statement,
            $.variable_declaration,
            $.expression_statement,
            ';'
        ),

        block_statement: $ => seq(
            '{',
            repeat($.statement),
            '}'
        ),
        return_statement: $ => seq('return', optional($._expression), ';'),
        break_statement: $ => seq('break', ';'),
        continue_statement: $ => seq('continue', ';'),
        if_statement: $ => prec.right(seq('if', $._expression, $.statement, optional(seq('else', $.statement)))),
        foreach_statement: $ => seq('for', $._for_each_varables, 'in', $._expression, $.statement),
        while_statement: $ => seq('while', $._expression, $.statement),
        expression_statement: $ => seq($._expression, ';'),


        variable_declaration: $ => seq(
            choice('var', 'val', 'static', 'global'),
            $.simple_name,
            optional(seq('as', $._type)),
            optional(seq('=', $._expression)),
            ';'
        ),

        _for_each_varables: $ => seq($.simple_name, repeat(seq(',', $.simple_name))),

        _expression: $ => choice(
            $.primary_expression,
            $.instanceof_expression,
            $.type_cast_expression,
            $.call_expression,
            $.member_index_expression,
            $.int_range_expression,
            $.member_access_expression,
            $.unary_expression,
            $.binary_expression,
            $.compare_expression,
            $.logical_expression,
            $.conditional_expression,
            $.assignment_expression,
        ),

        // begin expression
        bracket_handler_expression: $ => seq('<', token(repeat(/[^>]/)), '>'),
        parens_expression: $ => prec('parens', seq('(', $._expression, ')')),

        function_expression: $ => seq(
            'function',
            '(',
            alias(commaSep($._parameter_decl), $.parameter_list),
            ')',
            optional(seq('as', $._type)),
            $.function_body
        ),


        instanceof_expression: $ => prec('as', seq($._expression, 'instanceof', $._type)),
        type_cast_expression: $ => prec('as', seq($._expression, 'as', $._type)),
        call_expression: $ => prec('call', seq($._expression, '(', optional($._argument_list), ')')),

        _argument_list: $ => seq($._expression, repeat(seq(',', $._expression))),
        member_index_expression: $ => prec('index', seq($._expression, '[', $._expression, ']')),

        int_range_expression: $ => prec.left('range', choice(
            seq($._expression, 'to', $._expression),
            seq($._expression, '..', $._expression)
        )),

        member_access_expression: $ => prec('member', seq(
            $._expression,
            '.',
            alias(choice($.simple_name, 'string', $.string_literal), $.member_name),
        )),

        unary_expression: $ => prec('unary', choice(
            seq('!', $._expression),
            seq('-', $._expression),
        )),

        binary_expression: $ => choice(
            ...['*', '/', '%'].map(op => prec.left('mul', seq($._expression, op, $._expression))),
            ...['+', '-', '~'].map(op => prec.left('add', seq($._expression, op, $._expression))),

            prec.left('and', seq($._expression, '&', $._expression)),
            prec.left('xor', seq($._expression, '^', $._expression)),
            prec.left('or', seq($._expression, '|', $._expression)),
        ),

        compare_expression: $ => choice(
            ...['==', '!=', '<', '<=', '>', '>=', 'in', 'has'].map(op => prec.left(seq('compare', $._expression, op, $._expression))),
        ),


        logical_expression: $ => choice(
            prec.left('and_and', seq($._expression, '&&', $._expression)),
            prec.left('or_or', seq($._expression, '||', $._expression)),
        ),

        conditional_expression: $ => prec.right('conditional', seq($._expression, '?', $._expression, ':', $._expression)),


        _assign_lhs_expr: $ => choice(
            $.member_access_expression,
            $.member_index_expression,
            $.simple_name,
            $.parens_expression,
        ),

        assignment_expression: $ => choice(
            ...['=', '+=', '-=', '~=', '*=', '/=', '%=', '|=', '&=', '^=']
                .map(op => prec.right(seq($._assign_lhs_expr, op, $._expression))),
        ),

        primary_expression: $ => choice(
            $._primitive_literal,
            $.simple_name,
            $.function_expression,
            $.bracket_handler_expression,
            $.array_literal,
            $.map_literal,
            $.parens_expression,
        ),


        array_literal: $ => prec('literal', seq(
            '[',
            optional(seq(
                $._expression,
                repeat(seq(',', $._expression)),
                optional(','),
            )),
            ']'
        )),

        map_literal: $ => prec('literal', seq(
            '{',
            optional(seq(
                $.map_entry,
                repeat(seq(',', $.map_entry)),
                optional(','),
            )),
            '}'
        )),

        map_entry: $ => prec('literal', seq($._expression, ':', $._expression)),

        _primitive_literal: $ => choice(
            $.true,
            $.false,
            $.null,
            $.int_literal,
            $.float_literal,
            $.string_literal
        ),


        string_literal: $ => token(stringLiteral),
        int_literal: $ => token(intLiteral),
        float_literal: $ => token(floatLiteral),

        true: $ => 'true',
        false: $ => 'false',
        null: $ => 'null',

        // end expression


        _type: $ => choice(
            $._primitive_type,
            alias($.simple_name, $.class_type),
            $.function_type,
            $.list_type,
            $.array_type,
            $.map_type,
        ),

        // dzs
        _extend_type: $ => prec(-10, choice(
            $._type,
            $.intersection_type,
        )),

        _primitive_type: $ => choice(
            'any',
            'byte',
            'short',
            'int',
            'long',
            'float',
            'double',
            'bool',
            'void',
            'string',
        ),

        function_type: $ => prec.right(seq('function', '(', $._type_list, ')', $._type)),
        _type_list: $ => seq($._type, repeat(seq(',', $._type))),

        list_type: $ => seq('[', $._type, ']'),
        array_type: $ => prec('array_type', seq($._type, '[', ']')),
        map_type: $ => prec('map_type', seq($._type, '[', $._type, ']')),

        // dzs
        intersection_type: $ => seq($._type, repeat1(seq('&', $._type))),

        comment: $ => token(comment),
        preprocessor: $ => token(preprocessor),


        qualified_name: $ => seq($.simple_name, repeat(seq('.', $.simple_name))),

        simple_name: $ => prec('name', choice(
            $._identifier,
            'to',
        )),

        _identifier: $ => token(identifier)
    }
});