// Initialize express router
let router = require('express').Router();
// Set default API response
router.get('/', function (req, res) {
    res.json({
        status: 'API Is Working',
        message: 'Welcome to  contact data prediction!',
    });
});
// Import contact controller
var contactController = require('./contactController');
var exclusionController = require('./exclusionController');
// Contact routes
router.route('/contacts/predictionList/:dest')
    .get(contactController.index)
router.route('/contacts/predictionList/:dest/:exp')
    .get(contactController.view)
    //.post(contactController.new)
    .put(contactController.update)
    .delete(contactController.delete);

router.route('/contacts/exclusionList/:dest/:exp')
    .get(exclusionController.view)
    .put(exclusionController.update)
    .delete(exclusionController.delete);
// Export API routes
module.exports = router;