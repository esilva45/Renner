package br.com.renner.plugins;

import oracle.iam.identity.utils.Constants;

public class CustomConstants extends Constants {
	public static enum Field {

		CPF("cpf"), 
		RG_FUNCIONARIO("rg_funcionario"), 
		DOC_ESTRANGEIRO("doc_estrangeiro"), 
		DATADENASCIMENTO("DatadeNascimento");

		private String id;

		private Field(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}
	}
}
