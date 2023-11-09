
package org.openrefine.commands.history;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.history.ApplyOperationsCommand;

public class ApplyOperationsCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new ApplyOperationsCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
