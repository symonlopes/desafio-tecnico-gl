package com.desafiotecnico.subscription.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
    Todo erro que ocorrer na aplicação é retornada para o cliente consumidor da API um erro com essa mesta estrutura.
    O code serve para ser usado em testes para saber exatamente qual erro ocorreu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String code;
    private String description;
    private Object details;
}
