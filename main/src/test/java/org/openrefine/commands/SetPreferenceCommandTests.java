
package org.openrefine.commands;

import java.io.IOException;

import javax.servlet.ServletException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.commands.CommandTestBase;
import org.openrefine.commands.SetPreferenceCommand;

public class SetPreferenceCommandTests extends CommandTestBase {

    @BeforeMethod
    public void setUpCommand() {
        command = new SetPreferenceCommand();
    }

    @Test
    public void testCSRFProtection() throws ServletException, IOException {
        command.doPost(request, response);
        assertCSRFCheckFailed();
    }
}
